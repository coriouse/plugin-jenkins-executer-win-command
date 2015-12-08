package org.jenkinsci.plugins.stw;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import app.server.models.Message;

import javax.servlet.ServletException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TriggerBuilder extends Builder {

    private final String commands;
    
    final static String END_MESSAGE = "#";
    

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TriggerBuilder(String commands) {
        this.commands = commands;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getCommands() {
        return commands;
    }
    
    
    private Set<String> parsecommand(String commands) {
		Set<String> cmds = new HashSet<String>();
		String[] arrCmd = commands.split(",");
		for(String s : arrCmd) {
			cmds.add(s);
		}
		return cmds;
	}

	@SuppressWarnings("unchecked")
	private List<Message> getMessages(BufferedReader in) throws IOException {
		int value = 0;
		StringBuilder sb = new StringBuilder();
		while((value = in.read()) != -1) {
			sb.append((char)value);
		}
		return (List<Message>)Run.XSTREAM.fromXML(sb.toString());
	}
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	StringBuilder sb = new StringBuilder();
    	boolean isOk = true;
    	try{
	    	Socket echoSocket = new Socket(getDescriptor().getHost(), getDescriptor().getPort());
			PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			StringBuilder request = new StringBuilder();			
			request.append(Run.XSTREAM.toXML(new Message(Status.OK, Commands.EXECUTE,parsecommand(getCommands()))));			
			request.append(END_MESSAGE);				
			out.println(request);					
			List<Message> messages = getMessages(in);			
			for(Message m : messages) {
				if(m.status == Status.ERROR) {
					isOk = false;
					sb.append(m.description);
				}
				//System.out.println(m.status);
				//System.out.println(m.command);
				//System.out.println(m.description);
				//System.out.println(m.data);
				sb.append(m.data);
			}
    	} catch(Exception e) {
			StringBuffer message = new StringBuffer();
			message.append(e.getMessage()).append("\r\n");
			for(StackTraceElement t : e.getStackTrace()) {
				message.append(t.toString()).append("\r\t");
			}
    		listener.getLogger().println(message.toString());
			return false;
    	}	
    	listener.getLogger().println(sb.toString());   
    	if(!isOk) {
    		return false;
    	}
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link TriggerBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *     
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String host;        
        private int port;
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a host");            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckPort(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a port");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Add remote commands";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().            
            host = formData.getString("host");
            port = formData.getInt("port"); 		
            		
            // Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */            
        public String getHost() {
        	return host;
        }
        
        public int getPort() {
        	return port;
        }
    }
}

