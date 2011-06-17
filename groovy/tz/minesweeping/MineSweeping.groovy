/**
 * MineSweeping
 * @Author tz
 * @Email: 822112@qq.com
 * @Home: http://atian25.javaeye.com
 *
 * TODO: 
 *     1)add ico to mark/mine
 *     2)add customer level cfg
 *     3)add dragable mine layout
 */
package tz.minesweeping;

public class Main{
    static void main(args){
        def m = new Model();
        //m.isDebug =true;
        //def testData = "4,4,4,**203*31112*0011,0000000000000001";
        def swing = new MineSwingUI(m);
        def console = new MineConsoleUI(m);
        m.reset()
        //添加Console调用Swing的命令
        console.addCmdHandler("showui",[usage:"Show swing ui",check:{true},
            cmd:{
                delegate = swing 
                showUI();
            }]
        )
        console.addCmdHandler("hideui",[usage:"Hide swing ui",check:{true},
            cmd:{
                delegate = swing 
                hideUI();
            }]
        )
    }
}

/**
 * MineSweeping Model
 */
class Model extends Observable{
    //------Static Const
    //雷区
    def static MINE = "*";
    //空区域
    def static EMPTY = "0";
    //等待挖掘
    def static TODIG = "0";
    //已打开
    def static OPEN = "1";
    //标示为雷
    def static DANGEROUS = "2";
    //标示为疑问
    def static QUESTION = "3";

    //预配置
    def static LowCfg = "9,9,10";
    def static MiddleCfg = "16,16,40";
    def static HighCfg = "16,30,99";
    
    //获取状态的字面含义
    def static StatusDescript = [0:"TODIG",1:"OPEN",2:"DANGEROUS",3:"QUESTION"];
 
    //刷新事件,参数为[type:__,sizeChanged:__]
    def static REFRESH = "refreh event"
    //数据改变事件,参数为[type:__,index:__,value:__,oldValue:__,x:__,y:__]
    def static DATACHANGED = "data changed event"
    //状态改变事件,参数为[type:__,index:__,value:__,oldValue:__,x:__,y:__]
    def static STATUSCHANGED = "status changed event"
    //消息事件,参数为[type:__,msg:__,data:__]
    def static MSG = "msg event"
    //错误事件,参数为[type:__,msg:__,data:__]
    def static ERROR = "error event"
    //踩到雷事件,参数为[type:__,msg:__,x:__,y:__,index:__]
    def static LOSE = "lose event"
    //赢的事件,参数为[type:__,msg:__]
    def static WIN = "win event"
    
    //------Model Config
    int xlen = 9;
    int ylen = 9;
    int mineCount = 10;
    def isDebug = false;
    
    def dataList = [];
    def statusList = [];

    //------Var
    //已开启的格子数
    def openCount = 0;
    //是否踩到地雷
    def digMine = false;
    //是否暂停事件
    def skipEvent = false;
    
    //------Utils Method
    def isMine = {getData(it)==MINE};
    def isOpen = {getStatus(it)==OPEN};
    def isQuestion = {getStatus(it)==QUESTION};
    def isDangerous = {getStatus(it)==DANGEROUS};
    def isMark = {isDangerous(it)||isQuestion(it)};
    def canDig = {!isOpen(it) && !isDangerous(it) && !isQuestion(it)};
    def isLose = {digMine};
    def isWin = {!isLose() && openCount+mineCount==getSize() && getUnMarkMineCount()==0};
    
    def getSize = {xlen*ylen};
    def getData = {return dataList[it]};
    def getStatus = {return statusList[it]};
    def getCfg = {"$xlen,$ylen,$mineCount"};
    
    /**
     * 获取未标识为危险的雷的个数
     */
    def getUnMarkMineCount = {
        def sum = mineCount;
        statusList.eachWithIndex{elem,index->
            sum -= (isDangerous(index)?1:0)
        }
        return sum
    }
    
    /**
     * 根据坐标获取数组索引值
     */
    def getIndex = {int x, int y->
        return x+y*xlen;
    }
    
    /**
     * 根据数组索引值获取坐标
     */
    def getCoordinate = {int index->
        return [x:index%xlen,y:Math.floor(index/xlen).intValue()]
    }
    
    /**
     * 根据一个值,获取它不超出边界的邻居
     */
    def getRange = {int index,int len->
        return [index-1,index,index+1].grep{it>=0 && it<len};
    }
    
    /**
     * 根据index,获取它不超出边界的邻居的index列表
     */
    def getArounds = {int index,withoutOpen=false->
        def c = getCoordinate(index);
        [getRange(c.x,xlen),getRange(c.y,ylen)].combinations().collect{elem->
            getIndex(elem[0],elem[1])
        }.grep{elem->
            //过滤掉自己,且如果withoutOpen为true,则过滤掉状态为开启的
            elem!=index && !(withoutOpen && isOpen(elem))
        }
    }
    
     /**
     * 发布事件
     */
    def fireEvent = {type,arg->
        if(!skipEvent){
            setChanged();
            notifyObservers(["type":type]+arg);
        }
    }
    
    /**
     * 初始化状态和数据
     */
    def initEmpty = {
        //reset var
        openCount = 0;
        digMine = false;
        //clear list
        dataList.clear();
        statusList.clear();
        //fill empty box
        getSize().times{index->
           setData(index,EMPTY);
           setStatus(index,TODIG);
        }
        return this;
    }
    
    /**
     * 随机初始化雷区数据
     */
    def initMine = {
        def rand = new Random();
        def i=0;
        while(i<mineCount){
            def index = rand.nextInt(getSize());
            if(!isMine(index)){
                setData(index,MINE);
                i++;
            }
        }
        return this;
    }
    
    /**
     * 初始化指示牌
     */
    def initOther = {
        dataList.eachWithIndex{value,index->
            if(!isMine(index)){
                //计算周围的雷数
                def count = getArounds(index).inject(0){sum, i -> 
                    sum + (isMine(i) ? 1 : 0)
                };
                setData(index,count+"");
            }
        }
        return this;
    }
    
    /**
     * 改变标志状态
     */
    def setStatus = {int index, value->
        def old = statusList[index];
        statusList[index] = value;
        if(isOpen(index)){
            openCount++;
        }
        fireEvent(STATUSCHANGED,["index":index,"value":value,"oldValue":old]+getCoordinate(index));
        //println "mark ${getCoordinate(index)} as $value"
    }
    
    /**
     * 改变格子数据
     */
    def setData = {int index, value->
        def old = dataList[index];
        dataList[index] = value;
        fireEvent(DATACHANGED,["index":index,"value":value,"oldValue":old]+getCoordinate(index));
        //println "setData ${getCoordinate(index)} as $value"
    }
    
    /**
     * 导出数据
     */
    def exportData = {
        "$xlen,$ylen,$mineCount,${dataList.join('')},${statusList.join('')}"
    }
    
    /**
     * 刷新数据
     * 格式:
     * 1)"$xlen,$ylen,$mineCount,${dataList.join('')},${statusList.join('')}" <--载入数据
     * 2)"$xlen,$ylen,$mineCount" <--刷新尺寸并随机数据
     * 3) "" or null  <--仅随机数据
     */
    def reset = {
        def isSuc = false;
        def temp = it?.split(",")
        //是否尺寸发生改变
        def sizeChanged = false;
        if(!temp||!temp?.size()){
            //仅随机数据
            skipEvent = true;
            initEmpty();
            initMine();
            initOther();
            //发布事件
            skipEvent = false;
            fireEvent(REFRESH,["sizeChanged":sizeChanged]);
            isSuc = true;
        }else if(temp.size()==3){
            //刷新尺寸并随机数据
            def xsize = temp[0] as int;
            def ysize = temp[1] as int;
            sizeChanged = (xsize!=xlen || ysize!=ylen)
            //演示groovy1.6的多路赋值
            (xlen,ylen) = [xsize,ysize];
            mineCount = temp[2] as int;
            skipEvent = true;
            initEmpty();
            initMine();
            initOther();
            //发布事件
            skipEvent = false;
            fireEvent(REFRESH,["sizeChanged":sizeChanged]);
            isSuc = true;
        }else if(temp.size()==5){
            def xsize = temp[0] as int;
            def ysize = temp[1] as int;
            sizeChanged = (xsize!=xlen || ysize!=ylen)
            xlen = xsize;
            ylen = ysize;
            mineCount = temp[2] as int;
            def dataStr = temp[3];
            def statusStr = temp[4];
             //检查数据
            if(mineCount>=getSize()||dataStr.length()!=getSize()||statusStr.length()!=getSize()){
                fireEvent(ERROR,[msg:"loadData error,dataList/statusList/mineCount length not match the size.}",data:it])    
            }else{
                //暂停发布事件
                skipEvent = true;
                //初始化数据
                initEmpty();
                //填充数据
                getSize().times{index->
                    setData(index, dataStr[index])
                    setStatus(index,statusStr[index]) 
                }
                //重新计算其他格
                initOther();
                //发布事件
                skipEvent = false;
                fireEvent(REFRESH,["sizeChanged":sizeChanged]);
                isSuc = true;
            }
        }else{
            fireEvent(ERROR,[msg:"loadData error,data format should be: xlen,ylen,mineCount,dataList.join(''),statusList.join('').}",data:it])       
        }
        return isSuc;
    }

    
    /**
     * 挖雷
     * @Param cfg {map} : [index:__] or [x:__,y:__]
     */
    def dig = {cfg->
        if(cfg.index==null){
            cfg["index"] = getIndex(cfg.x as int,cfg.y as int)
        }else if(cfg.x==null||cfg.y==null){
            cfg+=getCoordinate(cfg.index);
        }else{
            fireEvent(ERROR,[msg:"dig params error, don't have index or x,y.cfg:$cfg"])        
        }
        def index = cfg.index;
        //已开启或标识的不能开启
        if(isOpen(index)||isMark(index)){
            fireEvent(MSG,[msg:"Can't dig opened/marked box at (${cfg.x},${cfg.y})"])    
        }else{
            //println "dig " + (cfg.index!=null ? getCoordinate(cfg.index) : "${cfg.x},${cfg.y}")
            setStatus(index,OPEN);
            if(isMine(index)){
                //踩到雷
                digMine = true;
                fireEvent(LOSE,[msg:"You got mine!",index:index]+getCoordinate(index))
                //println "You got mine!"
            }else if(getData(index)==EMPTY){
                //如果是空,则继续判断邻居的值
                def arr = getArounds(index,true);
                while(arr.size>0){
                    def i = arr.pop();
                    setStatus(i,OPEN);
                    //如果邻居也为空,则继续入栈邻居的未开启的邻居
                    if(getData(i)==EMPTY){
                        arr += getArounds(i,true)
                        arr.unique();
                    }
                }
            }
        }
        //检查是否赢了
        if(isWin()){
            fireEvent(WIN,[msg:"You win!"])
        }
        return this;
    }
    
    /**
     * 添加标识
     * @Param cfg {map} : [index:__] or [x:__,y:__]
     */
    def mark = {cfg->
        if(cfg.index==null){
            cfg["index"] = getIndex(cfg.x as int,cfg.y as int)
        }else if(cfg.x==null||cfg.y==null){
            cfg+=getCoordinate(cfg.index);
        }else{
            fireEvent(ERROR,[msg:"mark params error, don't have index or x,y.cfg:$cfg"])        
        }
        def index = cfg.index;
        switch(getStatus(index)){
            //已开启的不能加标识
            case OPEN:
                fireEvent(MSG,[msg:"Can't mark opened box at (${cfg.x},${cfg.y})"])
                break;
            case TODIG: 
                setStatus(index,DANGEROUS);
                break;
            case DANGEROUS:
                setStatus(index,QUESTION);
                break;
            case QUESTION:
                setStatus(index,TODIG);
                break;
        }
        //检查是否赢了
        if(isWin()){
            fireEvent(WIN,[msg:"You win!"])
        }
        return this;
    }
}



/**
 * Mine Console UI
 */
class MineConsoleUI implements Observer{
    private Model model;
    def logEvent = false;
    
    def read = new BufferedReader(new InputStreamReader(System.in)).&readLine;
    
    def MineConsoleUI(Model m){
        setModel(m);
        init();
    }
    
    def setModel = {model->
        this.model?.removeObserver(this)
        this.model = model
        model.addObserver(this)
    }
    
    /**
     * 初始化函数
     */
    def init = {
        //启动命令行
        Thread.start({
            try{
                while(true)cmdHandler()
                //userCmd()
            }catch(IOException e){
                println e
            }
        })
    }
    
    //------Event Handler
    /**
     * 事件处理函数分发器
     */
    public void update(Observable o, Object arg) {
        if(logEvent) println("$o,$arg")
        switch(arg.type){
            case Model.DATACHANGED:
                onDataChanged(o,arg);
                break;
            case Model.STATUSCHANGED:
                onStatusChanged(o,arg);
                break;
            case Model.REFRESH:
                onReset(o,arg);
                break;
            case Model.MSG:
            case Model.ERROR:
                println arg.msg;
                break;
            case Model.WIN:
                onWin(o,arg);
                break;
            case Model.LOSE:
                onLose(o,arg);
                break;
            default:
                onUnKnown(o,arg);
        }
    }
    
    def onReset = {o,arg->
        println("[Reset Model]")
        show();
    }
    
    def onDataChanged = {o,arg->
        println("[Change (${arg.x},${arg.y}) data from ${arg.oldValue} to ${arg.value}]")
    }
    
    def onStatusChanged = {o,arg->
        switch(arg.value){
            case Model.OPEN:
                println "dig (${arg.x},${arg.y})."
                break; 
            case Model.DANGEROUS:
                println "Mark (${arg.x},${arg.y}) as DANGEROUS!"
                break;
            case Model.QUESTION:
                println "Mark (${arg.x},${arg.y}) as QUESTION?"
                break;
            case Model.TODIG:
                println "Mark (${arg.x},${arg.y}) as TODIG."
                break;
            default: 
                println "Unknow Status Changed: $arg."
        }
    }
    
    def onWin = {o,arg->
        println "[You Win!!]"
        show();
    }
    
    def onLose = {o,arg->
        println "[You Lose!!]"
        show(true);
    }
    
    def onUnKnown = {o,arg->
       println("onUnKnown handler")
    }
    
    //------Cmd Helper
    //指令帮助
    def cmdMap = [
        show:[
            usage:"Show the matrix. Usage: show",
            check:{true},
            cmd:{show()}
        ],
        data:[
            usage:"Show the matrix with real data. Usage: data",
            check:{true},
            cmd:{show(true)}
        ],
        dig:[
            usage:"Open the box at(x,y). Usage:1)dig x y  2)dig x,y",
            check:{it.length>=3},
            cmd:{model.dig(["x":it[1] as int, "y":it[2] as int])}
        ],
        mark:[
            usage:"Mark the box at(x,y). Usage:1)mark x y  2)mark x,y",
            check:{it.length>=3},
            cmd:{model.mark(["x":it[1] as int, "y":it[2] as int])}
        ],
        reset:[
            usage:"Reset the game. Usage:1)reset  2)reset xlen,ylen,count",
            check:{it.length==1||it.length==4},
            cmd:{
                if(it.length==1){
                    model.reset()
                }else{
                    model.reset("${it[1]},${it[2]},${it[3]}")
                }
            }
        ],
        load:[
            usage:"Load Data. Usage:1)load [dataStr] *DataFormat should be: xlen,ylen,mineCount,dataList.join(''),statusList.join('').]",
            check:{it.length==6},
            cmd:{
                model.reset("${it[1]},${it[2]},${it[3]},${it[4]},${it[5]}")
            }
        ],
        export:[
            usage:"Export Data. Usage: export.  *DataFormat: xlen,ylen,mineCount,dataList.join(''),statusList.join('').]",
            check:{true},
            cmd:{
                println model.exportData()
            }
        ],
        debug:[
            usage:"enable/disable the debug mode. Usage:1)debug on  2)debug off",
            check:{it.length==2},
            cmd:{
                model.isDebug = (it[1]=="on")
                logEvent = model.isDebug
            }
        ],
        exit:[
            usage:"Exit program.",
            check:{true},
            cmd:{
                println "Bye~"
                System.exit(0)
            }
        ],
        help:[
            usage:"Get help. Usage:1)help  2)help [Cmd Name]",
            check:{it.length<=2},
            cmd:{
                if(it.length==2){
                    println "${cmdMap[it[1]]?.usage?:'Unknow cmd, type help to get help.'}"
                }else{
                    cmdMap.each{k,v->
                        println "[$k]:${v.usage}"
                    }
                }
            }
        ]
    ]
    
    /**
     * 用户输入交互处理
     */
    def cmdHandler = {
        print "\n>"
        def input = read();
        if(input){
            def params =  input?.split("\\s+|,")?:[];
            def cmdName = params[0];
            if(cmdMap.containsKey(cmdName)){
                def handler = cmdMap.get(cmdName);
                if(handler.check(params)){
                    handler.cmd(params);
                }else{
                    println handler.usage
                }
            }else{
                println "Unknow Cmd: $input, type help to get help."
            }
        }
    }
    
    /**
     * 添加用户命令
     */
    def addCmdHandler = {name,cfg->
        println "Add Cmd Handler:$name"
        cmdMap.put(name,cfg)
    }
    
    //------Console Handler
    /**
     * 计算Box显示的Label
     */
    def convertBoxLabel = {index,isDebug->
        def value = model.getData(index);
        //输出标识
        def str = isDebug ? "[${value}] " : "[ ] ";
        if(model.isQuestion(index)){
            str = isDebug ? "?${value}? " : "[?] "
        }else if(model.isDangerous(index)){
            str = isDebug ? "!${value}! " : "[!] "
        }else if(model.isOpen(index)){
            str = " $value  "
        }
        return str;
    }
    
    /**
     * 输出矩阵
     */ 
    def show = {showData=false->
        model.xlen.times{printf("====",it)}
        println "\nSize:${model.xlen}*${model.ylen}, Mine:${model.mineCount}"
        
        //输出横坐标
        print "     ";
        model.xlen.times{printf(" %-2d ",it)}
        println "";
        //输出间隔线
        print "   ";
        model.xlen.times{printf("----",it)}
        println "";
        
        //输出数据
        model.dataList.eachWithIndex{value,index->
            def c = model.getCoordinate(index);
            //输出纵坐标
            if(c.x==0) printf("%2d | ",c.y)
            //输出数据
            print convertBoxLabel(index,model.isDebug||showData);
            //边界换行
            if(c.x==model.xlen-1) println ""
        }
       
        //输出间隔线
        print "   ";
        model.xlen.times{printf("----",it)}
        println "";
    }
}



import groovy.swing.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.filechooser.*;
import java.beans.*;
import groovy.beans.Bindable;

/**
 * Mine Swing UI
 */
class MineSwingUI implements Observer{
    private Model model;
    @Bindable
    String currentCfg;
    
    //------UI Config
    def screenSize = Toolkit.getDefaultToolkit().getScreenSize()
    def btnSize = 45
    
    def swing = new SwingBuilder();
    JFrame frame;
    JPanel areaPanel;
    JMenuBar menuBar;
    JFileChooser fileChooserDialog;
    
    def MineSwingUI(Model m){
        setModel(m);
        init();
    }
    
    def setModel = {model->
        this.model?.removeObserver(this)
        this.model = model
        model.addObserver(this)
    }
        
    /**
     * 初始化函数
     */
    def init = {
        initUI();
    }
    
    //------Utils Method
    def getBoxId = {"box_$it"}
    def getBox = {swing."${getBoxId(it)}"}
    def getBoxIndex = {it.id.split(",")[1] as int}
    
    /**
     * 计算Box显示的Label
     */
    def convertBoxLabel = {index->
        def value = model.getData(index);
        //输出标识
        def str = model.isDebug ? "[${value}]" : " ";
        if(model.isQuestion(index)){
            str = model.isDebug ? "?${value}?" : "?"
        }else if(model.isDangerous(index)){
            str = model.isDebug ? "!${value}!" : "!"
        }else if(model.isOpen(index)){
            str = "$value"
        }
        return str;
    }
        
    /**
     * 计算StatusField显示的Label
     */
    def convertStatusLabel = {
        return "未标记的雷个数:${model.getUnMarkMineCount()}";
    }
        
    /**
     * 重玩
     */
    def replay = {msg,options=null->
        def result = JOptionPane.showConfirmDialog(null,"$msg 是否重玩?","是否重玩?",JOptionPane.YES_NO_OPTION)
        if(result==JOptionPane.YES_OPTION){
            model.reset(options)   
        }else{
            currentCfg = model.getCfg()
        }
    }
    
    /**
     * 提示信息
     */
    def showMsg = {msg->
        JOptionPane.showMessageDialog(null, msg); 
    }
    
    //------Event Handler
    /**
     * 事件处理函数分发器
     */
    public void update(Observable o, Object arg) {
        switch(arg.type){
            case Model.DATACHANGED:
                onDataChanged(o,arg);
                break;
            case Model.STATUSCHANGED:
                onStatusChanged(o,arg);
                break;
            case Model.REFRESH:
                onReset(o,arg);
                break;
            case Model.MSG:
                onMsg(o,arg);
                break;
            case Model.ERROR:
                onError(o,arg);
                break;
            case Model.WIN:
                onWin(o,arg);
                break;
            case Model.LOSE:
                onLose(o,arg);
                break;
            default:
                onUnKnown(o,arg);
        }
    }
    
    def onReset = {o,arg->
        currentCfg = model.getCfg();
        if(arg.sizeChanged){
            reinitArea();
        }else{
            resetArea();
        }
    }
    
    def onDataChanged = {o,arg->
        getBox(arg.index).text = convertBoxLabel(arg.index);
    }
    
    def onStatusChanged = {o,arg->
        getBox(arg.index).text = convertBoxLabel(arg.index);
        swing.statusField.text = convertStatusLabel();
        if(model.isOpen(arg.index)){
            getBox(arg.index).enabled = false;
        }
    }
    
    def onWin = {o,arg->
        replay("恭喜!你赢了!!")
    }
    
    def onLose = {o,arg->
        replay("你输了!!")
    }
    
    def onMsg = {o,arg->
        showMsg(arg.msg)
    }
    
    def onError = {o,arg->
        showMsg(arg.msg)
    }
    
    def onUnKnown = {o,arg->
       showMsg("onUnKnown handler")
    }

    //------UI Method
    /**
    * 初始化Swing UI
    */
    def initUI = {
        //如果存在frame,则关闭
        if(frame){
            frame.dispose();
        }
        
        //初始化UI
        frame = swing.frame(
            title:"::MineSweeping:: - by tz ",
            //size:[width,height],location:[x,y],
            pack:true,resizable:true,visible:true,
            defaultCloseOperation:WindowConstants.DO_NOTHING_ON_CLOSE //DISPOSE_ON_CLOSE
        ){
            lookAndFeel("system")
            //菜单
            genMenuBar();
            //主框
            panel(id:'mainPanel',layout:new BorderLayout()) {
                //North
                textField(id:"statusField",constraints:BorderLayout.NORTH,editable:false,text:convertStatusLabel())
            }
        }
        
        //退出方式
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                def result = JOptionPane.showOptionDialog(null,"隐藏窗口?关闭窗口?退出程序?","关闭方式?",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ["隐藏窗口","关闭窗口","退出程序","取消"] as Object[],"关闭窗口")
                if(result==0){
                    hideUI()
                }else if(result==1){
                    frame.dispose();
                }else if(result==1){
                    System.exit(0);
                }
            }
        });
        
        //绘制主框
        reinitArea()
        
        fileChooserDialog  = swing.fileChooser(dialogTitle:"Choose an excel file",fileSelectionMode : JFileChooser.FILES_ONLY){}

        //居中
        frame.setLocation((screenSize.width-frame.width)/2 as int,(screenSize.height - frame.height)/2 as int)
    }
    
    /**
     * 生成鼠标事件处理函数
     */
    def getMouseHandler = {index->
        return new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if(e.source.enabled && !model.isLose() && !model.isWin()){
                    if(e.getButton() == MouseEvent.BUTTON1){
                        model.dig(["index":index])
                    }else if(e.getButton() == MouseEvent.BUTTON3){
                        model.mark(["index":index])
                    }
                }
            }
        }
    }
    
    /**
     * 重新生成Area区域
     */
    def reinitArea = {
        if(areaPanel){
            swing.mainPanel.remove(areaPanel)
        }
        areaPanel = swing.panel(id:"areaPanel",constraints: BorderLayout.CENTER) {
            tableLayout {
                //遍历数据
                model.ylen.times{y->
                    //行
                    tr{
                        model.xlen.times{x->
                            //列
                            td{
                                def index = model.getIndex(x,y);
                                def id = getBoxId(index);
                                def btn = button(id:id,preferredSize:[btnSize,btnSize],focusPainted:false,text:convertBoxLabel(index)){
                                    //text:bind(source:this,sourceProperty:"obDataList",converter:{it[index]})
                                    //swing.action(name:"boxAction", closure:this.&boxAction)
                                }
                                btn.addMouseListener(getMouseHandler(index))
                            }
                        }
                    }
                }
            }
        }
        swing.mainPanel.add(areaPanel)
        frame.pack()
        frame.setLocation((screenSize.width-frame.width)/2 as int,(screenSize.height - frame.height)/2 as int)
    }
    
    /**
     * 重设Area
     */
    def resetArea = {
       model.getSize().times{
           def box = getBox(it)
           box.enabled = !model.isOpen(it);
           box.text = convertBoxLabel(it)
       }
       swing.statusField.text = convertStatusLabel();
    }
    
    /**
     * 生成菜单
     */
    def genMenuBar = {
        menuBar = swing.menuBar(id:'mb'){
            //菜单1
            menu(text: "游戏", mnemonic: 'G') {
                menuItem(text: "重玩", mnemonic: 'R', actionPerformed: {
                    replay("");
                })
                menuItem(text: "保存游戏", mnemonic: 'H', actionPerformed: {
                   fileChooserDialog.dialogTitle = "保存游戏"
                   if(fileChooserDialog.showDialog(frame,"保存")==JFileChooser.APPROVE_OPTION){
                        def file = fileChooserDialog.selectedFile
                        file.withWriter {out->
                            out.writeLine(model.exportData())
                        }
                        showMsg("保存成功!")
                    }
                })
                menuItem(text: "载入游戏", mnemonic: 'H', actionPerformed: {
                    fileChooserDialog.dialogTitle = "载入游戏"
                    if(fileChooserDialog.showDialog(frame,"载入")==JFileChooser.APPROVE_OPTION){
                        def file = fileChooserDialog.selectedFile
                        def data = ""
                        file.eachLine{
                           data+=it
                        }
                        def isSuc = model.reset(data);
                        showMsg("载入${isSuc?'成功':'失败'}!")
                    }
                })
                separator();
                menuItem(text: "隐藏窗口", mnemonic: 'H', actionPerformed: {
                    hideUI();
                })
                menuItem(text: "关闭窗口", mnemonic: 'C', actionPerformed: {
                    dispose();
                })
                menuItem(text: "退出", mnemonic: 'X', actionPerformed: {
                    dispose();
                    System.exit(0);
                })
            }
            //菜单2
            menu(text: "等级", mnemonic: 'L') {
                //TOFIX:根据model的cfg,更新selected
                buttonGroup().with{group->
                    radioButtonMenuItem(text: "低级", mnemonic: 'L',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.LowCfg}), actionPerformed: {
                        replay("",Model.LowCfg)
                    })
                    radioButtonMenuItem(text: "中级", mnemonic: 'M',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.MiddleCfg}), actionPerformed: {
                        replay("",Model.MiddleCfg)
                    })
                    radioButtonMenuItem(text: "高级", mnemonic: 'H',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.HighCfg}), actionPerformed: {
                        replay("",Model.HighCfg)
                    })
                    radioButtonMenuItem(text: "自定义...", mnemonic: 'C',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it!=Model.LowCfg&&it!=Model.MiddleCfg&&it!=Model.HighCfg}), actionPerformed: {
                        def inputValue = JOptionPane.showInputDialog(null,"自定义数据格式为:长,宽,雷数",model.getCfg());
                        if(inputValue){
                            if(inputValue=~/\d+,\d+,\d+/){
                                model.reset(inputValue);
                            }else{
                                showMsg("格式不正确!")
                            }
                        }
                    })
                }
            }
            //菜单3
            menu(text: "帮助", mnemonic: 'H'){
                menuItem(text: "游戏说明", mnemonic: 'H', actionPerformed: {
                    showMsg("* 方格里面的数字代表该方块周围8个方块中的地雷个数.\n* 左键翻开方格,右键标志为危险/疑问.\n* 翻开所有非雷方格后获胜,踩到雷则失败.\n* 该丢脸的说明是为大漠特地写的...可怜的大漠...")
                })
                menuItem(text: "关于...", mnemonic: 'T', actionPerformed: {e->
                    showMsg("MineSweeping\n* @Author tz\n* @Email: 822112@qq.com\n* @Home:http://atian25.javaeye.com")
                })
            }
        }  
    }
    
    /**
     * 显示UI
     */
    def showUI = {
        if(frame){
            frame.show()
        }else{
            initUI()    
        }
    }
    /**
     * 隐藏UI
     */
    def hideUI = {
        if(frame){
            frame.hide()
        }
    }
}