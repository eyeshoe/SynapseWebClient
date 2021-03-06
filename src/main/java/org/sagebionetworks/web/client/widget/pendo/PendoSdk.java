package org.sagebionetworks.web.client.widget.pendo;

import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.utils.Callback;

import com.google.gwt.core.shared.GWT;
import com.google.inject.Inject;

/**
 * Wrapper around the Pendo library
 * @author jayhodgson
 *
 */
public class PendoSdk {
	GWTWrapper gwt;
	Callback initializeCallback;
	String userId, email;
	@Inject
	public PendoSdk(GWTWrapper gwt) {
		this.gwt = gwt;
		this.initializeCallback = () -> {
			initialize(userId, email);
		};
	}

	public void initialize(String userId, String email) {
		this.userId = userId;
		this.email = email;
		if (_isLoaded()) {
			_initialize(userId, email);	
		} else {
			gwt.scheduleExecution(initializeCallback, 200);
		}
	}
	
	private static native boolean _isLoaded() /*-{
		return $wnd.pendo;
	}-*/;
	
	private static native void _initialize(
			String userId,
			String synapseEmail) /*-{
		// Call this whenever information about your visitors becomes available
	    // Please use Strings, Numbers, or Bools for value types.
	    try {
		    	$wnd.pendo.initialize({
		      visitor: {
		        id: userId,   // Required if user is logged in
		        email: synapseEmail// Optional
		        // role:         // Optional
		
		        // You can add any additional visitor level key-values here,
		        // as long as it's not one of the above reserved names.
		      },
		
		      account: {
		        // id:           'ACCOUNT-UNIQUE-ID' // Highly recommended
		        // name:         // Optional
		        // planLevel:    // Optional
		        // planPrice:    // Optional
		        // creationDate: // Optional
		
		        // You can add any additional account level key-values here,
		        // as long as it's not one of the above reserved names.
		      }
		    });
	    } catch(err) {
	    		console.error(err);
	    }
	}-*/;
}