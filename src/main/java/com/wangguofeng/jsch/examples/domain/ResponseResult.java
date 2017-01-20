package com.wangguofeng.jsch.examples.domain;


public class ResponseResult {
	private String exitCode;
    private String stderr;
    private String stdout;
    public ResponseResult() {
		
	}

    

	
	public String getStderr() {
		return stderr;
	}
	public String getExitCode() {
		return exitCode;
	}
    
	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}
	public void setStderr(String stderr) {
		this.stderr = stderr;
	}
	public String getStdout() {
		return stdout;
	}
	public void setStdout(String stdout) {
		this.stdout = stdout;
	}
    @Override
    public String toString(){
    	StringBuffer sb=new StringBuffer();
    	sb.append("exitCode:").append(exitCode).append("\n");
    	sb.append("stdout:").append(stdout).append("\n");
    	sb.append("stderr:").append(stderr);
    	return sb.toString();
    }
}
