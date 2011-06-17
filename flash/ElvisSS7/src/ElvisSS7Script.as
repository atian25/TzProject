// ActionScript file

import elvis.ss7.Logger;
import elvis.ss7.Model;

import flash.events.*;
import flash.filesystem.File;
import flash.net.*;

import mx.collections.ArrayCollection;
import mx.collections.ArrayList;
import mx.controls.DataGrid;
import mx.events.MenuEvent;
import mx.events.ListEvent;
import mx.formatters.*;

private var model:Model;
private var logger:Logger;

//方向数据
[Bindable]
private var directionData:ArrayCollection = new ArrayCollection([
	{label:"全部",data:'ALL'},
	{label:"上行",data:'up'},
	{label:"下行",data:'down'}
]);

//协议识别数据
[Bindable]
private var protocolData:ArrayCollection = new ArrayCollection([
	{label:"全部",data:'ALL'},
	{label:"DTAP Radio Resources Management Message Type",data:'01'}
]);

//消息类型数据
[Bindable]
private var msgTypeData:ArrayCollection = new ArrayCollection([
	{label:"全部",data:'ALL'},
	{label:"System Information Type 3 (0x1b)",data:'0x1b'}
]);

//原因值
[Bindable]
private var causeData:ArrayCollection = new ArrayCollection([
	{label:"全部",data:'ALL'}
]);

protected function onCreationCompleteHandler(e:Event):void{
	Logger.setLogPanel(this.logPanel);
	model = new Model();
	model.addEventListener(Model.DATA_CHANGED,dataChangedHandler);
	model.addEventListener(ProgressEvent.PROGRESS,progressHandler);
	model.loadData("D:\\TZProfile\\Documents\\Desktop\\eToneRTD132_0Terminal20091224093452375.log.xml");
	initMenu();
	initTimeField();
}

/**
 * 初始化菜单
 */
private function initMenu():void{
	var myMenuBarXML:XMLList =
		<>
		  <menuitem id="File" label="文件">
			<menuitem id="Open" label="打开"/>
			<menuitem id="History" label="最近打开">
			</menuitem>
		  </menuitem>
		  <menuitem id="Edit" label="编辑">
		  </menuitem>
		  <menuitem id="Help" label="帮助">
			<menuitem label="By TZ"/>
		  </menuitem>
		</>
		;
	this.menubar.dataProvider = myMenuBarXML;
	this.menubar.labelField = "@label";
	this.menubar.addEventListener(MenuEvent.ITEM_CLICK,menuItemClickHandler);
}

/**
 * 初始化时间选择
 */
private function initTimeField():void{
	var data:ArrayList = new ArrayList();
	for(var i:int = 0; i< 24;i++){
		for(var j:int = 0; j< 60;j+=5){
			var str:String = (i<10?'0'+i:i) + ':' + (j<10?'0'+j:j)
			data.addItem({label:str,data:str});
		}
	}
	this.startTimeField.dataProvider = data;
	this.finishTimeField.dataProvider = data;
}

/**
 * 打开文件选择对话框
 */ 
private function openFileDialog():void{
	Logger.log('b');
	var file:File = File.desktopDirectory;
	file.addEventListener(Event.SELECT,fileSelectHandler);
	file.browseForOpen("选择信令文件",[new FileFilter("Text/XML", "*.txt;*.xml")]);
}

/**
 * 文件选择处理函数
 */
private function fileSelectHandler(e:Event):void{
	var file:File = e.target as File;
	if(file.exists){
		Logger.log('Loading:'+file.nativePath);
		model.loadData(file.nativePath);
	}else{
		Logger.log('file no exists');
	}
}

/**
 * 菜单点击事件
 */
private function menuItemClickHandler(e:MenuEvent):void{
	if(e.item.@id=="Open"){
		openFileDialog();
	}
}

/**
 * 双击表格行事件处理函数
 */
private function dataGridDoubleClickHandler(e:ListEvent):void{
	var selectedRow:Object = e.currentTarget.selectedItem;
	Logger.log(selectedRow.msgType);
}

/**
 * 数据变更处理函数
 */
private function dataChangedHandler(e:Event):void{
	this.title = Model.PROGRAM_NAME + ' - ' + model.getFilePath();
	this.dataGrid.dataProvider = model.getSignalXmlList();
}

/**
 * 数据加载进度处理函数
 */
private function progressHandler(e:ProgressEvent):void{
	//Logger.log(e.bytesLoaded as String,e.bytesTotal as String)
	//this.progresser.setProgress(e.bytesLoaded,e.bytesTotal);
}

/**
 * DateField渲染
 */
private function dateLabelRenderer(date:Date):String{
	var df:DateFormatter=new DateFormatter();
	df.formatString="YY-MM-DD JJ:NN";
	return date==null? "": df.format(date);
}