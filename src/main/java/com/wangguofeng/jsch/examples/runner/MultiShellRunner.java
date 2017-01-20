package com.wangguofeng.jsch.examples.runner;

import static java.lang.Thread.sleep;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.wangguofeng.jsch.examples.domain.ResponseResult;
import com.wangguofeng.jsch.examples.domain.Server;
import com.wangguofeng.jsch.examples.domain.User;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public final class MultiShellRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MultiShellRunner.class);
    private static final  Pattern COLOR = Pattern.compile( "\\x1b\\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]");
    private static final Pattern REGEXP = Pattern.compile(  "[\\x00\\x08\\x0B\\x0C\\x0E-\\x1F]"); 
    private static final String HOSTNAMEPREFIX="MYIDANDHOSTNAME=";
    private static final String RESULTPREFIX="result=";
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
public static void main(String[] args) throws JSchException, InterruptedException, IOException {

	User user=new User("sam", "sam");
	
	   Server server=new Server();
	   server.setHost("10.28.1.97");

	   server.setPort(22);
	   String[]commands=new String[]{"cd /tmp","ls -la","pwd","a=1234","echo ${a}"};
    
	MultiShellRunner runner=new MultiShellRunner(server, user);
	List<ResponseResult>list=runner.execute(commands);
	  for(ResponseResult r:list){
		  System.out.println(r.getStdout());
	  	}
}

	private Session session;
    public MultiShellRunner(Server server,User user) {
             try {
                 Properties config = new Properties();
                 //config.putAll(props);
                 config.put("StrictHostKeyChecking", "no");
                 JSch jsch = new JSch();
                 Session newSession = jsch.getSession(user.getPassword(), server.getHost(),server.getPort());
                 newSession.setPassword(user.getPassword());
                 newSession.setUserInfo(user);
                 newSession.setDaemonThread(true);
                 newSession.setConfig(config);
                 newSession.connect(DEFAULT_CONNECT_TIMEOUT);
                 this.session=newSession;
             } catch (JSchException e) {
                 throw new RuntimeException("Cannot create session for " + server.getHost(), e);
             }
    }



private  String readLogin(PrintWriter pw, BufferedReader br) throws IOException{
	StringBuffer sb=new StringBuffer();
	String v=null;
	   while((v=br.readLine())!=null){
		   sb.append(v).append(System.lineSeparator());
		   if(v.toLowerCase().contains("Last login".toLowerCase())){
			   break;
		   }
	   }
	   return sb.toString();
	   
}
private  String getIdAndHostName( PrintWriter pw, BufferedReader br) throws IOException{
	String hostCommand="echo "+HOSTNAMEPREFIX+"$(whoami)'@'$(hostname)";
	pw.println(hostCommand);
    pw.flush();
    String hostName="";
    boolean end=false;
    while(true){
  	 String v=br.readLine();
    	  if(end){
    		  break;
    	  }
    	  if(v!=null&&v.startsWith(HOSTNAMEPREFIX)&&!v.equals(hostCommand)){
    		  hostName=v.replace(HOSTNAMEPREFIX, "");
    		  end=true;
    	  }
		   
   }
  
   return hostName;
}
private  ResponseResult  runScript( PrintWriter pw,String command, BufferedReader br) throws IOException{
	  	pw.println(command);
	  	String echoCommand="echo '"+RESULTPREFIX+"'$?";
	  	pw.println(echoCommand);
	  	pw.flush();
	boolean end=Boolean.FALSE;
	ResponseResult result=new ResponseResult();
	 List<String>list=new ArrayList<>();
	while(true){
		     String v=br.readLine();
		     //System.out.println(">>>"+v);
    		 if(end){
    			 if(v.startsWith(RESULTPREFIX)){
    				String statusCode=v.replace(RESULTPREFIX, "");
    				result.setExitCode(statusCode);
    			 }
    			 //System.out.println(v.replace("result=", ""));
    			  br.readLine();
    			
    			  break;
    		  }
			if(v.lastIndexOf(echoCommand)>0){
				end=true;
			}else{
			    v=COLOR.matcher(v).replaceAll("");
			    v=REGEXP.matcher(v).replaceAll("");
			    list.add(v);
			}
		}

	  result.setStdout( StringUtils.join(list.subList(0, list.size()-1), System.lineSeparator()));
	
   return result;
}

public List<ResponseResult> execute(String[]commands) throws JSchException, InterruptedException, IOException {
	
	List<ResponseResult> list=new ArrayList<>();
	
    Channel channel = this.session.openChannel("shell");
    try(InputStream in=channel.getInputStream();
            OutputStream out= channel.getOutputStream();
            PrintWriter pw = new PrintWriter(out);){
    	channel.connect();
        channel.start();
        BufferedReader br=new BufferedReader(new InputStreamReader(in));
    	  String loginStr=readLogin(pw,br);
    	 //System.out.println(loginStr);
    	  String hostPrefix=getIdAndHostName(pw,br);
       // System.out.println(hostPrefix);
        for(String command:commands){
      	  ResponseResult result= runScript(pw,command,br);
      	  list.add(result);
      	  if(!result.getExitCode().equalsIgnoreCase("0")){
      		  break;
      	  }
        }
        pw.println("exit");
        pw.flush();

       while (!channel.isEOF()) {
          sleep(100);
       }
    }finally{
    	channel.disconnect();
    	this.session.disconnect();
    }
    return list;
 
}
 
}