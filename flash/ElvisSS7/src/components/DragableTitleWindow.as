package components{
	import spark.components.TitleWindow;
	
	public class DragableTitleWindow extends TitleWindow{
		public function DragableTitleWindow(){
			super();
		}
		private var _dragEnabled:Boolean = false;	
		
		public function set dragEnabled(value:Boolean):void{
			this._dragEnabled = value;
			this.isPopUp = value;	//设置 dragEnabled 的值时, 同时更改 isPopUp 的值
		}
		
		public function get dragEnabled():Boolean{
			return this._dragEnabled;
		}

	}
}