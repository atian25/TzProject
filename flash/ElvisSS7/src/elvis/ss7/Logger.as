package elvis.ss7{
	import spark.components.TextArea;

	public class Logger{
		private static var logPanel:TextArea;
		public function Logger(){
		}
		public static function setLogPanel(p:TextArea):void{
			logPanel = p;
		}
		public static function log(msg:String,...rest):void{
			var str:String = msg;
			for(var i:int=0;i<rest.length;i++){
				str += ','+rest[i];
			}
			logPanel.appendText(str+'\n');
		}
	}
}