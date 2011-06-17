package elvis.ss7{
	import flash.events.*;
	import flash.filesystem.*;
	import flash.net.*;
	public class Model extends EventDispatcher{
		public static const PROGRAM_NAME:String = '拨测信令分析程序';
		public static const DATA_CHANGED:String = 'Data Changed';
		private var filePath:String;
		private var xml:XML;
		private var loader:URLLoader = new URLLoader();
		public function Model(target:IEventDispatcher=null){
			super(target);
			loader = new URLLoader();
			loader.addEventListener(Event.COMPLETE,dataLoaded);
			loader.addEventListener(IOErrorEvent.IO_ERROR,dataLoadError);
			loader.addEventListener(ProgressEvent.PROGRESS,dataLoading);
		}
		
		public function loadData(path:String):void{
			var request:URLRequest = new URLRequest(path);
			this.filePath = path;
			loader.load(request);
		}

		/**
		 * ====================================
		 * 数据提取函数
		 * ====================================
		 */
		
		/**
		 * 返回SignalXml节点列表
		 * @return 数组,格式:{
					id:node.@id,
					timestamp:node.@timestamp,
					direction:node.@direction,
					protocol:node.@protocol_discriminator,
					msgType:node.@message_type,
					cause:node.@cause
				}
		 */
		public function getSignalXmlList():Array{
			var arr:Array = [];
			var xmlList:XMLList = xml..signalxml;
			for (var pname:String in xmlList){
				var node:XML = xmlList[pname];
				var obj:Object = {
					id:node.@id,
					timestamp:node.@timestamp,
					direction:node.@direction,
					protocol:node.@protocol_discriminator,
					msgType:node.@name,
					//msgType:node.@message_type,
					cause:node.@cause
				};
				arr.push(obj);
			}
			return arr;
		}
		
		/**
		 * ====================================
		 * 事件处理函数
		 * ====================================
		 */
		private function dataLoaded(e:Event):void{
			Logger.log('load ok');	
			xml = XML(loader.data);
			//Logger.log('signalxml.length=',signalxml.length(),signalxml[0]);
			this.dispatchEvent(new Event(Model.DATA_CHANGED));
		}
		
		private function dataLoadError(e:IOErrorEvent):void{
		}
		
		private function dataLoading(e:ProgressEvent):void{
			this.dispatchEvent(e);			
		}
		
		/**
		 * ====================================
		 * 辅助函数
		 * ====================================
		 */
		public function getFilePath():String{
			return this.filePath;
		}
	}
}