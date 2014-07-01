package com.evernote.client.oauth.android;

import java.io.File;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.conn.mobile.TEvernoteHttpClient;
import com.evernote.client.oauth.EvernoteAuthToken;
import com.evernote.edam.notestore.NoteStore;

public class EvernoteSessionEx {

	  private ApplicationInfo applicationInfo;
	  private AuthenticationResult authenticationResult;
	  private File tempDir;

	  /**
	   * Create a new EvernoteSession that is not initially authenticated.
	   * To authenticate, call {@link #authenticate()}.
	   *
	   * @param applicationInfo The information required to authenticate.
	   * @param tempDir A directory in which temporary files can be created.
	   */
	  public EvernoteSessionEx(ApplicationInfo applicationInfo, File tempDir) {
	    this.applicationInfo = applicationInfo;
	    this.tempDir = tempDir;
	  }

	  /**
	   * Create a new Evernote session using saved information
	   * from a previous successful authentication.
	   */
	  public EvernoteSessionEx(ApplicationInfo applicationInfo,
	      AuthenticationResult sessionInfo, File tempDir) {
	    this(applicationInfo, tempDir);
	    this.authenticationResult = sessionInfo;
	  }

	  public AuthenticationResult getAuthenticationResult() {
		  return this.authenticationResult;
	  }
	  /**
	   * Check whether the session has valid authentication information
	   * that will allow successful API calls to be made.
	   */
	  public boolean isLoggedIn() {
	    return authenticationResult != null;
	  }

	  /**
	   * Clear all stored authentication information.
	   */
	  public void logOut() {
	    authenticationResult = null;
	    EvernoteOAuthActivityEx.authToken = null;
	  }

	  /**
	   * Get the authentication token that is used to make API calls
	   * though a NoteStore.Client.
	   *
	   * @return an authentication token, or null if {@link #isLoggedIn()}
	   * is false.
	   */
	  public String getAuthToken() {
	    if (authenticationResult != null) {
	      return authenticationResult.getAuthToken();
	    } else {
	      return null;
	    }
	  }

	  /**
	   * Get a new NoteStore Client. The returned client can be used for any
	   * number of API calls, but is NOT thread safe.
	   *
	   * @throws IllegalStateException if @link #isLoggedIn() is false.
	   * @throws TTransportException if an error occurs setting up the
	   * connection to the Evernote service.
	   */
	  public NoteStore.Client createNoteStore() throws TTransportException {
	    if (!isLoggedIn()) {
	      throw new IllegalStateException();
	    }
	    TEvernoteHttpClient transport =
	      new TEvernoteHttpClient(authenticationResult.getNoteStoreUrl(),
	          applicationInfo.getUserAgent(), tempDir);
	    TBinaryProtocol protocol = new TBinaryProtocol(transport);
	    return new NoteStore.Client(protocol, protocol);
	  }

	  /**
	   * Start the OAuth authentication process. Obtains an OAuth request token
	   * from the Evernote service and redirects the user to the web browser
	   * to authorize access to their Evernote account.
	   */
	  public void authenticate(Context context) {
	    // Create an activity that will be used for authentication
	    Intent intent = new Intent(context, EvernoteOAuthActivityEx.class);
	    intent.putExtra(EvernoteOAuthActivityEx.EXTRA_EVERNOTE_HOST, applicationInfo.getEvernoteHost());
	    intent.putExtra(EvernoteOAuthActivityEx.EXTRA_CONSUMER_KEY, applicationInfo.getConsumerKey());
	    intent.putExtra(EvernoteOAuthActivityEx.EXTRA_CONSUMER_SECRET, applicationInfo.getConsumerSecret());
	    if (!(context instanceof Activity)) {
	      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    }
	    context.startActivity(intent);
	  }

	  /**
	   * Complete the OAuth authentication process after the user authorizes
	   * access to their Evernote account and the Evernote service redirects
	   * the user back to the application.
	   */
	  public boolean completeAuthentication() {
	    if (EvernoteOAuthActivityEx.authToken != null) {
	      EvernoteAuthToken token = EvernoteOAuthActivityEx.authToken;
	      authenticationResult =
	        new AuthenticationResult(token.getToken(), token.getNoteStoreUrl(),
	            token.getWebApiUrlPrefix(), token.getUserId());
	      return true;
	    } else {
	      // If there's a pending authentication and we have no auth token, we failed
	      boolean result = !EvernoteOAuthActivityEx.startedAuthentication;
	      return result;
	    }
	  }
}
