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
        //���Console����Swing������
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
    //����
    def static MINE = "*";
    //������
    def static EMPTY = "0";
    //�ȴ��ھ�
    def static TODIG = "0";
    //�Ѵ�
    def static OPEN = "1";
    //��ʾΪ��
    def static DANGEROUS = "2";
    //��ʾΪ����
    def static QUESTION = "3";

    //Ԥ����
    def static LowCfg = "9,9,10";
    def static MiddleCfg = "16,16,40";
    def static HighCfg = "16,30,99";
    
    //��ȡ״̬�����溬��
    def static StatusDescript = [0:"TODIG",1:"OPEN",2:"DANGEROUS",3:"QUESTION"];
 
    //ˢ���¼�,����Ϊ[type:__,sizeChanged:__]
    def static REFRESH = "refreh event"
    //���ݸı��¼�,����Ϊ[type:__,index:__,value:__,oldValue:__,x:__,y:__]
    def static DATACHANGED = "data changed event"
    //״̬�ı��¼�,����Ϊ[type:__,index:__,value:__,oldValue:__,x:__,y:__]
    def static STATUSCHANGED = "status changed event"
    //��Ϣ�¼�,����Ϊ[type:__,msg:__,data:__]
    def static MSG = "msg event"
    //�����¼�,����Ϊ[type:__,msg:__,data:__]
    def static ERROR = "error event"
    //�ȵ����¼�,����Ϊ[type:__,msg:__,x:__,y:__,index:__]
    def static LOSE = "lose event"
    //Ӯ���¼�,����Ϊ[type:__,msg:__]
    def static WIN = "win event"
    
    //------Model Config
    int xlen = 9;
    int ylen = 9;
    int mineCount = 10;
    def isDebug = false;
    
    def dataList = [];
    def statusList = [];

    //------Var
    //�ѿ����ĸ�����
    def openCount = 0;
    //�Ƿ�ȵ�����
    def digMine = false;
    //�Ƿ���ͣ�¼�
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
     * ��ȡδ��ʶΪΣ�յ��׵ĸ���
     */
    def getUnMarkMineCount = {
        def sum = mineCount;
        statusList.eachWithIndex{elem,index->
            sum -= (isDangerous(index)?1:0)
        }
        return sum
    }
    
    /**
     * ���������ȡ��������ֵ
     */
    def getIndex = {int x, int y->
        return x+y*xlen;
    }
    
    /**
     * ������������ֵ��ȡ����
     */
    def getCoordinate = {int index->
        return [x:index%xlen,y:Math.floor(index/xlen).intValue()]
    }
    
    /**
     * ����һ��ֵ,��ȡ���������߽���ھ�
     */
    def getRange = {int index,int len->
        return [index-1,index,index+1].grep{it>=0 && it<len};
    }
    
    /**
     * ����index,��ȡ���������߽���ھӵ�index�б�
     */
    def getArounds = {int index,withoutOpen=false->
        def c = getCoordinate(index);
        [getRange(c.x,xlen),getRange(c.y,ylen)].combinations().collect{elem->
            getIndex(elem[0],elem[1])
        }.grep{elem->
            //���˵��Լ�,�����withoutOpenΪtrue,����˵�״̬Ϊ������
            elem!=index && !(withoutOpen && isOpen(elem))
        }
    }
    
     /**
     * �����¼�
     */
    def fireEvent = {type,arg->
        if(!skipEvent){
            setChanged();
            notifyObservers(["type":type]+arg);
        }
    }
    
    /**
     * ��ʼ��״̬������
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
     * �����ʼ����������
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
     * ��ʼ��ָʾ��
     */
    def initOther = {
        dataList.eachWithIndex{value,index->
            if(!isMine(index)){
                //������Χ������
                def count = getArounds(index).inject(0){sum, i -> 
                    sum + (isMine(i) ? 1 : 0)
                };
                setData(index,count+"");
            }
        }
        return this;
    }
    
    /**
     * �ı��־״̬
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
     * �ı��������
     */
    def setData = {int index, value->
        def old = dataList[index];
        dataList[index] = value;
        fireEvent(DATACHANGED,["index":index,"value":value,"oldValue":old]+getCoordinate(index));
        //println "setData ${getCoordinate(index)} as $value"
    }
    
    /**
     * ��������
     */
    def exportData = {
        "$xlen,$ylen,$mineCount,${dataList.join('')},${statusList.join('')}"
    }
    
    /**
     * ˢ������
     * ��ʽ:
     * 1)"$xlen,$ylen,$mineCount,${dataList.join('')},${statusList.join('')}" <--��������
     * 2)"$xlen,$ylen,$mineCount" <--ˢ�³ߴ粢�������
     * 3) "" or null  <--���������
     */
    def reset = {
        def isSuc = false;
        def temp = it?.split(",")
        //�Ƿ�ߴ緢���ı�
        def sizeChanged = false;
        if(!temp||!temp?.size()){
            //���������
            skipEvent = true;
            initEmpty();
            initMine();
            initOther();
            //�����¼�
            skipEvent = false;
            fireEvent(REFRESH,["sizeChanged":sizeChanged]);
            isSuc = true;
        }else if(temp.size()==3){
            //ˢ�³ߴ粢�������
            def xsize = temp[0] as int;
            def ysize = temp[1] as int;
            sizeChanged = (xsize!=xlen || ysize!=ylen)
            //��ʾgroovy1.6�Ķ�·��ֵ
            (xlen,ylen) = [xsize,ysize];
            mineCount = temp[2] as int;
            skipEvent = true;
            initEmpty();
            initMine();
            initOther();
            //�����¼�
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
             //�������
            if(mineCount>=getSize()||dataStr.length()!=getSize()||statusStr.length()!=getSize()){
                fireEvent(ERROR,[msg:"loadData error,dataList/statusList/mineCount length not match the size.}",data:it])    
            }else{
                //��ͣ�����¼�
                skipEvent = true;
                //��ʼ������
                initEmpty();
                //�������
                getSize().times{index->
                    setData(index, dataStr[index])
                    setStatus(index,statusStr[index]) 
                }
                //���¼���������
                initOther();
                //�����¼�
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
     * ����
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
        //�ѿ������ʶ�Ĳ��ܿ���
        if(isOpen(index)||isMark(index)){
            fireEvent(MSG,[msg:"Can't dig opened/marked box at (${cfg.x},${cfg.y})"])    
        }else{
            //println "dig " + (cfg.index!=null ? getCoordinate(cfg.index) : "${cfg.x},${cfg.y}")
            setStatus(index,OPEN);
            if(isMine(index)){
                //�ȵ���
                digMine = true;
                fireEvent(LOSE,[msg:"You got mine!",index:index]+getCoordinate(index))
                //println "You got mine!"
            }else if(getData(index)==EMPTY){
                //����ǿ�,������ж��ھӵ�ֵ
                def arr = getArounds(index,true);
                while(arr.size>0){
                    def i = arr.pop();
                    setStatus(i,OPEN);
                    //����ھ�ҲΪ��,�������ջ�ھӵ�δ�������ھ�
                    if(getData(i)==EMPTY){
                        arr += getArounds(i,true)
                        arr.unique();
                    }
                }
            }
        }
        //����Ƿ�Ӯ��
        if(isWin()){
            fireEvent(WIN,[msg:"You win!"])
        }
        return this;
    }
    
    /**
     * ��ӱ�ʶ
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
            //�ѿ����Ĳ��ܼӱ�ʶ
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
        //����Ƿ�Ӯ��
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
     * ��ʼ������
     */
    def init = {
        //����������
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
     * �¼��������ַ���
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
    //ָ�����
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
     * �û����뽻������
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
     * ����û�����
     */
    def addCmdHandler = {name,cfg->
        println "Add Cmd Handler:$name"
        cmdMap.put(name,cfg)
    }
    
    //------Console Handler
    /**
     * ����Box��ʾ��Label
     */
    def convertBoxLabel = {index,isDebug->
        def value = model.getData(index);
        //�����ʶ
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
     * �������
     */ 
    def show = {showData=false->
        model.xlen.times{printf("====",it)}
        println "\nSize:${model.xlen}*${model.ylen}, Mine:${model.mineCount}"
        
        //���������
        print "     ";
        model.xlen.times{printf(" %-2d ",it)}
        println "";
        //��������
        print "   ";
        model.xlen.times{printf("----",it)}
        println "";
        
        //�������
        model.dataList.eachWithIndex{value,index->
            def c = model.getCoordinate(index);
            //���������
            if(c.x==0) printf("%2d | ",c.y)
            //�������
            print convertBoxLabel(index,model.isDebug||showData);
            //�߽绻��
            if(c.x==model.xlen-1) println ""
        }
       
        //��������
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
     * ��ʼ������
     */
    def init = {
        initUI();
    }
    
    //------Utils Method
    def getBoxId = {"box_$it"}
    def getBox = {swing."${getBoxId(it)}"}
    def getBoxIndex = {it.id.split(",")[1] as int}
    
    /**
     * ����Box��ʾ��Label
     */
    def convertBoxLabel = {index->
        def value = model.getData(index);
        //�����ʶ
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
     * ����StatusField��ʾ��Label
     */
    def convertStatusLabel = {
        return "δ��ǵ��׸���:${model.getUnMarkMineCount()}";
    }
        
    /**
     * ����
     */
    def replay = {msg,options=null->
        def result = JOptionPane.showConfirmDialog(null,"$msg �Ƿ�����?","�Ƿ�����?",JOptionPane.YES_NO_OPTION)
        if(result==JOptionPane.YES_OPTION){
            model.reset(options)   
        }else{
            currentCfg = model.getCfg()
        }
    }
    
    /**
     * ��ʾ��Ϣ
     */
    def showMsg = {msg->
        JOptionPane.showMessageDialog(null, msg); 
    }
    
    //------Event Handler
    /**
     * �¼��������ַ���
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
        replay("��ϲ!��Ӯ��!!")
    }
    
    def onLose = {o,arg->
        replay("������!!")
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
    * ��ʼ��Swing UI
    */
    def initUI = {
        //�������frame,��ر�
        if(frame){
            frame.dispose();
        }
        
        //��ʼ��UI
        frame = swing.frame(
            title:"::MineSweeping:: - by tz ",
            //size:[width,height],location:[x,y],
            pack:true,resizable:true,visible:true,
            defaultCloseOperation:WindowConstants.DO_NOTHING_ON_CLOSE //DISPOSE_ON_CLOSE
        ){
            lookAndFeel("system")
            //�˵�
            genMenuBar();
            //����
            panel(id:'mainPanel',layout:new BorderLayout()) {
                //North
                textField(id:"statusField",constraints:BorderLayout.NORTH,editable:false,text:convertStatusLabel())
            }
        }
        
        //�˳���ʽ
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                def result = JOptionPane.showOptionDialog(null,"���ش���?�رմ���?�˳�����?","�رշ�ʽ?",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ["���ش���","�رմ���","�˳�����","ȡ��"] as Object[],"�رմ���")
                if(result==0){
                    hideUI()
                }else if(result==1){
                    frame.dispose();
                }else if(result==1){
                    System.exit(0);
                }
            }
        });
        
        //��������
        reinitArea()
        
        fileChooserDialog  = swing.fileChooser(dialogTitle:"Choose an excel file",fileSelectionMode : JFileChooser.FILES_ONLY){}

        //����
        frame.setLocation((screenSize.width-frame.width)/2 as int,(screenSize.height - frame.height)/2 as int)
    }
    
    /**
     * ��������¼�������
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
     * ��������Area����
     */
    def reinitArea = {
        if(areaPanel){
            swing.mainPanel.remove(areaPanel)
        }
        areaPanel = swing.panel(id:"areaPanel",constraints: BorderLayout.CENTER) {
            tableLayout {
                //��������
                model.ylen.times{y->
                    //��
                    tr{
                        model.xlen.times{x->
                            //��
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
     * ����Area
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
     * ���ɲ˵�
     */
    def genMenuBar = {
        menuBar = swing.menuBar(id:'mb'){
            //�˵�1
            menu(text: "��Ϸ", mnemonic: 'G') {
                menuItem(text: "����", mnemonic: 'R', actionPerformed: {
                    replay("");
                })
                menuItem(text: "������Ϸ", mnemonic: 'H', actionPerformed: {
                   fileChooserDialog.dialogTitle = "������Ϸ"
                   if(fileChooserDialog.showDialog(frame,"����")==JFileChooser.APPROVE_OPTION){
                        def file = fileChooserDialog.selectedFile
                        file.withWriter {out->
                            out.writeLine(model.exportData())
                        }
                        showMsg("����ɹ�!")
                    }
                })
                menuItem(text: "������Ϸ", mnemonic: 'H', actionPerformed: {
                    fileChooserDialog.dialogTitle = "������Ϸ"
                    if(fileChooserDialog.showDialog(frame,"����")==JFileChooser.APPROVE_OPTION){
                        def file = fileChooserDialog.selectedFile
                        def data = ""
                        file.eachLine{
                           data+=it
                        }
                        def isSuc = model.reset(data);
                        showMsg("����${isSuc?'�ɹ�':'ʧ��'}!")
                    }
                })
                separator();
                menuItem(text: "���ش���", mnemonic: 'H', actionPerformed: {
                    hideUI();
                })
                menuItem(text: "�رմ���", mnemonic: 'C', actionPerformed: {
                    dispose();
                })
                menuItem(text: "�˳�", mnemonic: 'X', actionPerformed: {
                    dispose();
                    System.exit(0);
                })
            }
            //�˵�2
            menu(text: "�ȼ�", mnemonic: 'L') {
                //TOFIX:����model��cfg,����selected
                buttonGroup().with{group->
                    radioButtonMenuItem(text: "�ͼ�", mnemonic: 'L',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.LowCfg}), actionPerformed: {
                        replay("",Model.LowCfg)
                    })
                    radioButtonMenuItem(text: "�м�", mnemonic: 'M',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.MiddleCfg}), actionPerformed: {
                        replay("",Model.MiddleCfg)
                    })
                    radioButtonMenuItem(text: "�߼�", mnemonic: 'H',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it==Model.HighCfg}), actionPerformed: {
                        replay("",Model.HighCfg)
                    })
                    radioButtonMenuItem(text: "�Զ���...", mnemonic: 'C',buttonGroup:group,selected:bind(source:this,sourceProperty:'currentCfg',converter:{it!=Model.LowCfg&&it!=Model.MiddleCfg&&it!=Model.HighCfg}), actionPerformed: {
                        def inputValue = JOptionPane.showInputDialog(null,"�Զ������ݸ�ʽΪ:��,��,����",model.getCfg());
                        if(inputValue){
                            if(inputValue=~/\d+,\d+,\d+/){
                                model.reset(inputValue);
                            }else{
                                showMsg("��ʽ����ȷ!")
                            }
                        }
                    })
                }
            }
            //�˵�3
            menu(text: "����", mnemonic: 'H'){
                menuItem(text: "��Ϸ˵��", mnemonic: 'H', actionPerformed: {
                    showMsg("* ������������ִ���÷�����Χ8�������еĵ��׸���.\n* �����������,�Ҽ���־ΪΣ��/����.\n* �������з��׷�����ʤ,�ȵ�����ʧ��.\n* �ö�����˵����Ϊ��Į�ص�д��...�����Ĵ�Į...")
                })
                menuItem(text: "����...", mnemonic: 'T', actionPerformed: {e->
                    showMsg("MineSweeping\n* @Author tz\n* @Email: 822112@qq.com\n* @Home:http://atian25.javaeye.com")
                })
            }
        }  
    }
    
    /**
     * ��ʾUI
     */
    def showUI = {
        if(frame){
            frame.show()
        }else{
            initUI()    
        }
    }
    /**
     * ����UI
     */
    def hideUI = {
        if(frame){
            frame.hide()
        }
    }
}