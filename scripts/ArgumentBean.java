import java.util.ArrayList;


public class ArgumentBean {

	public static ArrayList<String> inputArguments = new ArrayList<String>(); 
	
	public void getArguments(String[] args) {
		
		for(int i=0; i<args.length; i++){
			inputArguments.add(args[i]);
		}
		
	}

	public boolean doesHave(String s) {
		boolean b = true;
		if(inputArguments.contains(s)){
			b = true;
		}else{
			b = false;
		}
		
		return b;
	}

	public String returnValueOf(String s) {
		return inputArguments.get(inputArguments.indexOf(s)+1);
	}
	
	
	
	
	
	
}
