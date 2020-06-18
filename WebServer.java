/*--------------------------------------------------------

 Chadwick Rivera-Crum 




----------------------------------------------------------*/
import java.io.*; //imports all java libraries related to In/Out
import java.net.*;//The libraries used for networking such as Socket. 
import java.util.*;//imports important libraries for useful functions such as File. 

class Worker extends Thread { 
	/*
	This worked is spawned off when the MyWebServer receives a connection from the web browser. The worker creates an instance of a socket 
	and then accepts input from the browser. This input is know to initially start with a GET statement. This statement is broken apart to extract
	 the filename that is attempting to be retrieved by the browser. The following lines are ignored. If the file is html, txt or java it is then passed 
	 to the method handleFile(). handleFile opens the file and retrieves the content and then using the PrintStream output to send the content back to
	 the browser with the correct formatting. 
	*/
	
	Socket sock;//This creates an instance of socket to form a connection stream with the browser. 
	
	Worker (Socket s) { sock = s; } 

	public void run(){
		/* 
		This is the true workhorse of the worker class. It starts by opening a in and out stream from the browser. It then retrieves input from 
		the browser and splits the GET message to retrieve the fileName and type. 
		*/
		PrintStream out = null; //ensuring out is initialized to something. 
		BufferedReader in = null; 
	
		try {
			in = new BufferedReader
				(new InputStreamReader(sock.getInputStream()));
				//This is the input stream to retrieve browser information such as the GET message. 
			out = new PrintStream(sock.getOutputStream());
			//What will be used to send output to browser and passed to other methods. 
			try {
				/*
				This try-block starts by splitting the GET message using spaces. This will separate GET, fileName.type, HTTP. The second element 
				of the array is the fileName.type and that is used further to differentiate which method will be used. If the fileType contains
				.fake-cgi, we know that we will be using the addNums method. If the fileType contains html, java or txt then it is known that 
				we will be handling/opening a file such as dog.txt. Lastly, if instead the passed file is a directory, it is send to createDirectory 
				so that the directory can be displayed to the browser/user. 
				*/
				 String input;//string that stores the browser input
				 String fileName;//a string that will store the extracted Filename. 
				 int nameStart;//this will allow us to remove the leading '/'
				 int end;//marks the end of the fileName.
				 int dotIndex;//Marks the dot which separates fileName from fileType
				 String fileType;//stores the fileType string to be used in handleFile
				 String directoryName;//stores the directory name. 

				 input=in.readLine();
				 if (input != null){
				 	/*
				 	Splits the browser input and creates a string array which contains [GET FileName.type HTTP]. This is an initial implementation that
				 	may be removed, but leaving it to further functionality and increase precision. 
				 	*/
				 	String[] inputSplit=input.split(" ");
				 	nameStart=1+input.indexOf('/');//gives the index at which the fileName starts after leading '/'
				 	dotIndex=input.indexOf('.');//index where dot starts in FileName.type
				 	end=input.lastIndexOf(' ');//index of end of FileName.type. 


        			 if (inputSplit[1].indexOf(".fake-cgi")>=0){//LOGIC: if fileName includes .fake-cgi then it requesting 
        			 	//behavior of addNums
        				addNums(out, inputSplit[1]);//sends input which will include Person, Num1, Num2 to addNums to be extracted.
        			}
        			else if ( (input.indexOf(".html")>=0) || (input.indexOf(".txt")>=0) || (input.indexOf(".java")>=0)){
   						/*
   						LOGIC: if html OR txt OR java is found in input, then a file is trying to be retrieved.
   						*/
   						System.out.println(input);
        				fileName=input.substring(nameStart, end);
        				fileType=input.substring(dotIndex, end);
        				handleFile(fileName, fileType, out);//sends fileName, type and output stream to handleFile. 
        			}
        			else if (input.indexOf("favicon")>=0) { }//behavior to ignore FAVICON

        			else if (input.indexOf('/')!=input.lastIndexOf('/')){
        				//LOGIC: this needs to be updated to a better assumption, but if the last index of / is different than the first then 
        				//a directory is being accessed
        				System.out.println(input);
        				directoryName=inputSplit[1];
        				if (directoryName.contains("..") || directoryName.contains("../..") ){
        					/* Security feature, if a directory outside of the "home" directory is trying to be accessed or 
        					contains ../.. to access server directory, then simple display security concern. 
        					*/
        					System.out.println("This is deemed a security concern");
        				}
        				else{
        					//if no security concern, then send directoryName to directory method. 
        				createDirectory(out, directoryName);}
        			}
        			

        			else System.out.println(input);//for testing, to print what is not getting caught be if/else statements.
        			System.out.flush ();

        			}

    		

				
				
				
			} catch (IOException x){
				System.out.println("Server read error");
				x.printStackTrace(); //Provides an error read-out if there is an issue with In/Output with inner try block.  
			}
			sock.close(); //closes a connection with client. This seems to solve issues with data display. 
		} catch (IOException ioe) { System.out.println(ioe);}//Catches In/Output issues with establishing streams. 
	}
	public static void handleFile(String fileName, String fileType, PrintStream out){
		/*
		This method handles file types html, txt and java. The FileInputStream is first established to get input from the given file. 
		An instance of file is then created so that File length can be passed to the browser as part of output message. 
		Then an instance of bufferedReader is created so that the lines of the file can be parsed in the while loop and sent to the browser. 
		*/
		try{

		FileInputStream in= new FileInputStream(fileName);//creates input stream from file for BufferedReader. 
		File file=new File(fileName);
		BufferedReader fileContent=new BufferedReader(new InputStreamReader(in));
		String fileLine;
		if (file.canRead()) {
			//ensures file can be read. 
		

		if (fileType.equals(".html")){
			//formats output for html Content-Type
			System.out.println("Sending text/html file titled: "+ fileName);
			out.print("HTTP/1.1 200 OK"+ "Content-Type: text/html"+"Content-Length: "+ file.length()+ "Connection: close"+ "\r\n\r\n");
		}
		
		else{
			//formats output for Java/Txt. 
			System.out.println("Sending text/plain file titled: "+ fileName);
			out.print("HTTP/1.1 200 OK"+ "Content-Type: text/plain"+"Content-Length: "+ file.length()+"Connection: close"+"\r\n\r\n");
		}

		while ((fileLine=fileContent.readLine())!=null){
			//reads file line by line and sends to browser. 
			//System.out.println(fileLine);
			out.println(fileLine);
			out.flush();
		}
		
		out.flush();
		in.close();//closes filestream. 


		}
		
	} catch (Exception e){System.out.println("error");}

	out.close();//closes output stream to resolve data loading issue.

	}


	public static void createDirectory(PrintStream out, String directoryName){
	/*
	Takes output stream and directory as arguments. Prints a message for serverlog. An instance of file is then created
	using the directory. An array of File is created which contains a list of the files to be traversed within the for-loop. 
	*/
	System.out.println("Looking up directory: "+ directoryName);
    
    File file = new File ( "." + directoryName ) ;
    String parent=file.getParent();//to display parent link (aka to go back)

    // retrieves all files from directory. This code was provided by Professor Elliott. 
    File[] strFilesDirs = file.listFiles ( );
    //prepares output for correct Content-type.
	out.print("HTTP/1.1 200 OK"+ "Content-Type: text/html"+"Content-Length: "+ file.length()+ "Connection: close"+ "\r\n\r\n");
	out.print("<pre>");
	out.print("<h1> Index of Directory:"+ directoryName+ "</h1>");


	//out.print("<h1> Parent: "+ file.getParentFile()+ " and "+file.getParentFile()+ "</h1>");
	if(file.getParentFile()!=null){
		//places a "go back" button on the page, but ensures it won't be NULL
		out.println("<a href=http://localhost:2540/"+ parent+">"+"Go Back</a> <br>");}
    for ( int i = 0 ; i < strFilesDirs.length ; i ++ ) {
    	//a loop that traverses the files of the directory and sends them to browser with hotlink.
    	
      if ( strFilesDirs[i].isDirectory ( ) ){ 
	System.out.println ( "Directory: " + strFilesDirs[i] );//prints directories in traversal.
      	
	out.print("<a href="+strFilesDirs[i]+">"+strFilesDirs[i].getName()+"/</a> <br>");
	}
      else if ( strFilesDirs[i].isFile ( ) ){
	System.out.println ( "File: " + strFilesDirs[i] + 
			      " (" + strFilesDirs[i].length ( ) + ")" );//prints file traversal.
      	
	out.print("<a href=/"+strFilesDirs[i]+">"+strFilesDirs[i].getName()+"</a> <br>");
	}
	out.flush();
	}
}

	public static void addNums(PrintStream out, String input){
		/*
		This is the method that makes it seem like we are doing backend processing. 
		However, the input is actually from the GET request and extracted from the fileName that is trying to be retrieved. 
		In a somewhat dirty manner, the different indexes of the = and & are found which separate the data in the string. 
		These indexes are then used to create an output and calculate the sum. 
		*/
		System.out.println("Received AddNum Request and calculating..." + input);
		out.print("HTTP/1.1 200 OK"+ "Content-Type: text/html"+"Content-Length: "+ input.length()+ "Connection: close"+ "\r\n\r\n");
		//GET /cgi/addnums.fake-cgi?person=YourName&num1=4&num2=5 HTTP/1.1 (this is what will be sent in GET request.)
		String[] split= input.split(" ");//splits the input which is GET request. 
		input=split[0];//unsure why this index is the one that works as it should be [1]
		int firstEqual=-1, secondEqual=-1, thirdEqual=-1, firstAm=-1, secondAm=-1;//initializes to -1 as a flag 
		
		for(int i=0; i<input.length();i++){
			//this loops through the input string and extracts the indexes to be used for accessing the data
			//this implementation only works if it is known where the information is going to be.
			if (input.charAt(i)=='='){
				if (firstEqual==-1)
					firstEqual=i;
				else if (secondEqual==-1)
					secondEqual=i;
				else if (thirdEqual==-1)
					thirdEqual=i;
			}
			else if(input.charAt(i)=='&'){
				if (firstAm==-1) firstAm=i;
				else if (secondAm==-1) secondAm=i;
			}
					
		}
		String person=input.substring(firstEqual+1, firstAm);//the name of the person exists between the first = and first &
		int num1=Integer.parseInt(input.substring(secondEqual+1, secondAm));//first integer is parsed to integer from a string. 
		int num2=Integer.parseInt(input.substring(thirdEqual+1));
		int result=num1+num2;//completes "processing" of integers. 
		
		out.print("<html> <body> <h1> Hello, "+person+"</h1> <h1> The answer is: " +result + "</h1> </body> </html>");
		//sends out with information to browser.


		
	}







}
public class MyWebServer{
	/*
	This is primarily the INET and JokeServer code. A socket is created at port 2540. The accepted connection is then sent 
	to Worker class. 
	*/

	public static void main(String a[]) throws IOException{
		int q_len = 6;//number of queued simultaneous connections.
		int port = 2540; //port that server will accept connections. 
		Socket sock; //instace of sock to be passed to worker. 

		ServerSocket servsock = new ServerSocket(port, q_len);//creates port connection for listening.
		System.out.println
			("Chadwick Rivera-Crum's MyWebServer 1.8 starting up, listening at port 2540.\n");//prints to server log. 
		while (true){//a loop so that requests are continuously accepted.  
			sock = servsock.accept();//accepts an incoming connection and stores it in sock so that is can be passed to worker. 
			new Worker(sock).start();//spawns a new thread of Worker for this specific request. 
		}
	}
}
















