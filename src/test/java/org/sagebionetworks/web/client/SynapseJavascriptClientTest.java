package org.sagebionetworks.web.client;

import static com.google.gwt.http.client.RequestBuilder.GET;
import static com.google.gwt.http.client.RequestBuilder.POST;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException.TOO_MANY_REQUESTS_STATUS_CODE;
import static org.sagebionetworks.web.client.SynapseJavascriptClient.*;
import static org.sagebionetworks.web.shared.WebConstants.FILE_SERVICE_URL_KEY;
import static org.sagebionetworks.web.shared.WebConstants.REPO_SERVICE_URL_KEY;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.SynapseJavascriptFactory.OBJECT_TYPE;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.shared.WebConstants;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.asynch.AsynchType;
import org.sagebionetworks.web.shared.exceptions.BadRequestException;
import org.sagebionetworks.web.shared.exceptions.ConflictException;
import org.sagebionetworks.web.shared.exceptions.ConflictingUpdateException;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.LockedException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.shared.exceptions.ResultNotReadyException;
import org.sagebionetworks.web.shared.exceptions.SynapseDownException;
import org.sagebionetworks.web.shared.exceptions.TooManyRequestsException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
public class SynapseJavascriptClientTest {
	SynapseJavascriptClient client;
	private static SynapseJavascriptFactory synapseJsFactory = new SynapseJavascriptFactory();
	private static JSONObjectAdapter jsonObjectAdapter = new JSONObjectAdapterImpl();
	public static final String REPO_ENDPOINT = "http://repo-endpoint/v1";
	public static final String FILE_ENDPOINT = "http://file-endpoint/v1";
	public static final String USER_SESSION_TOKEN = "abc123";
	
	@Mock
	PortalGinInjector mockGinInjector;
	@Mock
	RequestBuilderWrapper mockRequestBuilder;
	@Mock
	AuthenticationController mockAuthController;
	@Mock
	GWTWrapper mockGwt;
	@Mock
	SynapseJSNIUtils mockJsniUtils;
	@Mock
	SynapseProperties mockSynapseProperties;
	@Captor
	ArgumentCaptor<RequestCallback> requestCallbackCaptor;
	@Mock
	AsyncCallback mockAsyncCallback;
	@Mock
	Request mockRequest;
	@Mock
	Response mockResponse;
	@Captor
	ArgumentCaptor<Throwable> throwableCaptor;
	@Captor
	ArgumentCaptor<Callback> callbackCaptor;
	@Captor
	ArgumentCaptor<String> stringCaptor;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		when(mockSynapseProperties.getSynapseProperty(REPO_SERVICE_URL_KEY)).thenReturn(REPO_ENDPOINT);
		when(mockSynapseProperties.getSynapseProperty(FILE_SERVICE_URL_KEY)).thenReturn(FILE_ENDPOINT);
		when(mockGinInjector.getRequestBuilder()).thenReturn(mockRequestBuilder);
		when(mockGinInjector.getAuthenticationController()).thenReturn(mockAuthController);
		client = new SynapseJavascriptClient(
				jsonObjectAdapter, 
				mockSynapseProperties, 
				mockGwt,
				synapseJsFactory,
				mockGinInjector,
				mockJsniUtils);
	}
	
	@Test
	public void testGetException() {
		String reason = "error message";
		assertTrue(getException(SC_UNAUTHORIZED, reason) instanceof UnauthorizedException);
		assertTrue(getException(SC_FORBIDDEN, reason) instanceof ForbiddenException);
		assertTrue(getException(SC_NOT_FOUND, reason) instanceof NotFoundException);
		assertTrue(getException(SC_BAD_REQUEST, reason) instanceof BadRequestException);
		assertTrue(getException(SC_LOCKED, reason) instanceof LockedException);
		assertTrue(getException(SC_PRECONDITION_FAILED, reason) instanceof ConflictingUpdateException);
		assertTrue(getException(SC_GONE, reason) instanceof BadRequestException);
		assertTrue(getException(SynapseTooManyRequestsException.TOO_MANY_REQUESTS_STATUS_CODE, reason) instanceof TooManyRequestsException);
		assertTrue(getException(SC_SERVICE_UNAVAILABLE, reason) instanceof SynapseDownException);
		assertTrue(getException(SC_CONFLICT, reason) instanceof ConflictException);
		assertTrue(getException(-1, reason) instanceof UnknownErrorException);
	}

	@Test
	public void testGetEntityBundleAnonymousSuccess() throws RequestException, JSONObjectAdapterException {
		String entityId = "syn291";
		int partsMask = 22;
		client.getEntityBundle(entityId, partsMask, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/" + entityId + BUNDLE_MASK_PATH + partsMask;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder, never()).setHeader(eq(SESSION_TOKEN_HEADER), anyString());
		
		verify(mockRequestBuilder).sendRequest(eq((String)null), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		EntityBundle testBundle = new EntityBundle();
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		testBundle.writeToJSONObject(adapter);
		when(mockResponse.getStatusCode()).thenReturn(SC_OK);
		when(mockResponse.getText()).thenReturn(adapter.toJSONString());
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onSuccess(testBundle);
	}
	
	@Test
	public void testGetEntityBundleForVersionLoggedInFailure() throws RequestException, JSONObjectAdapterException {
		String entityId = "syn291";
		int partsMask = 22;
		Long versionNumber = 5L;
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		
		client.getEntityBundleForVersion(entityId, versionNumber, partsMask, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/" + entityId + REPO_SUFFIX_VERSION + "/" + versionNumber + BUNDLE_MASK_PATH + partsMask;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
		
		verify(mockRequestBuilder).sendRequest(eq((String)null), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		
		when(mockResponse.getStatusCode()).thenReturn(SC_FORBIDDEN);
		String statusText = "user is not allowed access";
		when(mockResponse.getStatusText()).thenReturn(statusText);
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onFailure(throwableCaptor.capture());
		Throwable t = throwableCaptor.getValue();
		assertTrue(t instanceof ForbiddenException);
		assertEquals(statusText, t.getMessage());
	}
	
	@Test
	public void testGetReasonFromFailure() throws RequestException, JSONObjectAdapterException {
		String entityId = "syn291";
		int partsMask = 22;
		Long versionNumber = 5L;
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		client.getEntityBundleForVersion(entityId, versionNumber, partsMask, mockAsyncCallback);
		
		verify(mockRequestBuilder).sendRequest(eq((String)null), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		
		when(mockResponse.getStatusCode()).thenReturn(SC_BAD_REQUEST);
		String statusText = "Bad Request";
		String reason = "The results of this query exceeded the maximum number of allowable bytes: 512000.";
		String responseText = "{\"reason\":" + "\""+reason+"\"}";
		when(mockResponse.getStatusText()).thenReturn(statusText);
		when(mockResponse.getText()).thenReturn(responseText);
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onFailure(throwableCaptor.capture());
		Throwable t = throwableCaptor.getValue();
		assertTrue(t instanceof BadRequestException);
		assertEquals(reason, t.getMessage());
	}
	
	@Test
	public void testGetTeamWithRetry() throws RequestException, JSONObjectAdapterException {
		String teamId = "9123";
		client.getTeam(teamId, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + TEAM + "/" + teamId;
		verify(mockRequestBuilder).configure(GET, url);
		
		verify(mockRequestBuilder).sendRequest(eq((String)null), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		
		//simulate too many requests
		when(mockResponse.getStatusCode()).thenReturn(TOO_MANY_REQUESTS_STATUS_CODE);
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		//verify we'll try again later
		verify(mockGwt).scheduleExecution(callbackCaptor.capture(), eq(INITIAL_RETRY_REQUEST_DELAY_MS));
		//simulate retry
		callbackCaptor.getValue().invoke();
		verify(mockRequestBuilder, times(2)).sendRequest(eq((String)null), any(RequestCallback.class));
	}
	
	@Test
	public void testPostRestrictionInformation() throws RequestException, JSONObjectAdapterException {
		String subjectId = "syn9898782";
		RestrictableObjectType type = RestrictableObjectType.ENTITY;
		client.getRestrictionInformation(subjectId, type, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + RESTRICTION_INFORMATION;
		verify(mockRequestBuilder).configure(POST, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(WebConstants.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).sendRequest(stringCaptor.capture(), requestCallbackCaptor.capture());
		
		//verify request data
		String json = stringCaptor.getValue();
		RestrictionInformationRequest request = new RestrictionInformationRequest(jsonObjectAdapter.createNew(json));
		assertEquals(subjectId, request.getObjectId());
		assertEquals(type, request.getRestrictableObjectType());
	}
	
	@Test
	public void testPostEntityChildrenLoggedInWithRetry() throws RequestException, JSONObjectAdapterException {
		EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
		entityChildrenRequest.setParentId("syn982");
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		
		client.getEntityChildren(entityChildrenRequest, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + CHILDREN;
		verify(mockRequestBuilder).configure(POST, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(WebConstants.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
		
		verify(mockRequestBuilder).sendRequest(stringCaptor.capture(), requestCallbackCaptor.capture());
		String originalRequestString = stringCaptor.getValue();
		// (no need to verify request object is correct, that's in another test)
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		//simulate too many requests
		when(mockResponse.getStatusCode()).thenReturn(TOO_MANY_REQUESTS_STATUS_CODE);
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		//verify we'll try again later
		verify(mockGwt).scheduleExecution(callbackCaptor.capture(), eq(INITIAL_RETRY_REQUEST_DELAY_MS));
		//simulate retry
		callbackCaptor.getValue().invoke();
		verify(mockRequestBuilder, times(2)).sendRequest(stringCaptor.capture(), requestCallbackCaptor.capture());
		//verify it retries the same request
		assertEquals(originalRequestString, stringCaptor.getValue());
		
		//verify exponential backoff
		requestCallback = requestCallbackCaptor.getValue();
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		verify(mockGwt).scheduleExecution(callbackCaptor.capture(), eq(INITIAL_RETRY_REQUEST_DELAY_MS*2));
	}
	
	@Test
	public void testGetVersionOfV2WikiPageAsV1() throws RequestException, JSONObjectAdapterException {
		WikiPageKey key = new WikiPageKey();
		String pageId = "222";
		String ownerObjectId = "syn9834";
		String ownerObjectType = ObjectType.ENTITY.name().toLowerCase();
		Long versionNumber = 42L;
		key.setWikiPageId(pageId);
		key.setOwnerObjectId(ownerObjectId);
		key.setOwnerObjectType(ownerObjectType);
		
		client.getVersionOfV2WikiPageAsV1(key, versionNumber, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + "/" + ownerObjectType + "/" + ownerObjectId + WIKI + pageId + WIKI_VERSION_PARAMETER + versionNumber;
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testGetUserGroupHeadersByPrefix() throws RequestException, JSONObjectAdapterException {
		String prefix = "hello";
		when(mockGwt.encodeQueryString(anyString())).thenReturn(prefix);
		TypeFilter typeFilter = TypeFilter.TEAMS_ONLY;
		long limit = 10;
		long offset = 0;
		
		client.getUserGroupHeadersByPrefix(prefix, typeFilter, limit, offset, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + 
			USER_GROUP_HEADER_PREFIX_PATH + 
			prefix + "&" + 
			LIMIT_PARAMETER + limit +  "&" + 
			OFFSET_PARAMETER + offset + 
			TYPE_FILTER_PARAMETER + typeFilter.name();
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testPostListUserProfiles() throws RequestException, JSONObjectAdapterException {
		String userId1 = "32";
		String userId2 = "8";
		List<String> userIds = new ArrayList<>();
		userIds.add(userId1);
		userIds.add(userId2);
		
		client.listUserProfiles(userIds, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + USER_PROFILE_PATH;
		verify(mockRequestBuilder).configure(POST, url);
		
		verify(mockRequestBuilder).sendRequest(anyString(), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		
		//we can use ListWrapper in junit (java) since it is not compiled into js.
		ListWrapper<UserProfile> results = new ListWrapper<UserProfile>(UserProfile.class);
		List<UserProfile> profiles = new ArrayList<UserProfile>();
		UserProfile profile = new UserProfile();
		profile.setOwnerId(userId1);
		profiles.add(profile);
		profile = new UserProfile();
		profile.setOwnerId(userId2);
		profiles.add(profile);
		results.setList(profiles);
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		results.writeToJSONObject(adapter);
		when(mockResponse.getStatusCode()).thenReturn(SC_OK);
		when(mockResponse.getText()).thenReturn(adapter.toJSONString());
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onSuccess(profiles);
	}
	
	@Test
	public void testGetFavorites() throws RequestException, JSONObjectAdapterException {
		client.getFavorites(mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + 
			FAVORITE_URI_PATH + 
			"?" +
			OFFSET_PARAMETER + "0&" +
			LIMIT_PARAMETER + "200";
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testGetUserBundle() throws RequestException, JSONObjectAdapterException {
		Long principalId = 8222L;
		int mask = 23;
		client.getUserBundle(principalId, mask, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + 
			USER + "/" + principalId + 
			BUNDLE_MASK_PATH + mask;
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testGetOpenMembershipInvitationCount() throws RequestException, JSONObjectAdapterException {
		client.getOpenMembershipInvitationCount(mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + 
			OPEN_MEMBERSHIP_INVITATION_COUNT; 
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testGetOpenMembershipRequestCount() throws RequestException, JSONObjectAdapterException {
		client.getOpenMembershipRequestCount(mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + 
			OPEN_MEMBERSHIP_REQUEST_COUNT; 
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	@Test
	public void testGetEntity()  throws RequestException, JSONObjectAdapterException {
		String entityId = "syn921";
		client.getEntity(entityId, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + 
			ENTITY + "/" + entityId;
		verify(mockRequestBuilder).configure(GET, url);
	}
	
	public void testGetForEntity()  throws RequestException, JSONObjectAdapterException {
		String entityId = "syn921";
		Long versionNumber = 3L;
		client.getEntityForVersion(entityId, versionNumber, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + 
			ENTITY + "/" + entityId +
			REPO_SUFFIX_VERSION + "/" + versionNumber;
		verify(mockRequestBuilder).configure(GET, url);
	}

	@Test
	public void testGetNewEntityInstance() throws RequestException, JSONObjectAdapterException, ResultNotReadyException {
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		new FileEntity().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof FileEntity);
		
		adapter = jsonObjectAdapter.createNew();
		new Folder().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof Folder);
		
		adapter = jsonObjectAdapter.createNew();
		new EntityView().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof EntityView);
		
		adapter = jsonObjectAdapter.createNew();
		new TableEntity().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof TableEntity);
		
		adapter = jsonObjectAdapter.createNew();
		new Project().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof Project);
	
		adapter = jsonObjectAdapter.createNew();
		new Link().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof Link);
		
		adapter = jsonObjectAdapter.createNew();
		new Preview().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof Preview);
		
		adapter = jsonObjectAdapter.createNew();
		new DockerRepository().writeToJSONObject(adapter);
		assertTrue(synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter) instanceof DockerRepository);

	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetInvalidEntityInstance() throws RequestException, JSONObjectAdapterException, ResultNotReadyException {
		// if using OBJECT_TYPE Entity, then json must represent a recognized subclass of Entity.
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		new Team().writeToJSONObject(adapter);
		synapseJsFactory.newInstance(OBJECT_TYPE.Entity, adapter);
	}
	
	@Test
	public void testGetFileHandleAndUrlBatch() throws RequestException, JSONObjectAdapterException {
		BatchFileRequest fileRequest = new BatchFileRequest();
		client.getFileHandleAndUrlBatch(fileRequest, mockAsyncCallback);
		//verify url and method
		String url = FILE_ENDPOINT + FILE_HANDLE_BATCH;
		verify(mockRequestBuilder).configure(POST, url);
		
		verify(mockRequestBuilder).sendRequest(anyString(), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		
		//simulate "created" response (which is what this service returns if successful
		BatchFileResult result = new BatchFileResult();
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		result.writeToJSONObject(adapter);
		when(mockResponse.getStatusCode()).thenReturn(SC_CREATED);
		when(mockResponse.getText()).thenReturn(adapter.toJSONString());
		
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onSuccess(result);
	}
	
	@Test
	public void testUpdateV2WikiPage() throws RequestException, JSONObjectAdapterException {
		String ownerObjectId = "syn123";
		String wikiPageId = "282";
		WikiPageKey pageKey = new WikiPageKey();
		pageKey.setOwnerObjectId(ownerObjectId);
		pageKey.setOwnerObjectType(ObjectType.ENTITY.toString());
		pageKey.setWikiPageId(wikiPageId);
		
		V2WikiPage page = new V2WikiPage();
		page.setId(wikiPageId);
		client.updateV2WikiPage(pageKey, page, mockAsyncCallback);

		//verify url and method
		String url = REPO_ENDPOINT + "/" + pageKey.getOwnerObjectType().toLowerCase() + "/" + ownerObjectId + WIKI2 + wikiPageId;
		verify(mockRequestBuilder).configure(PUT, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(WebConstants.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).sendRequest(stringCaptor.capture(), requestCallbackCaptor.capture());
		
		//verify request data
		String json = stringCaptor.getValue();
		V2WikiPage request = new V2WikiPage(jsonObjectAdapter.createNew(json));
		assertEquals(wikiPageId, request.getId());
	}
	
	@Test
	public void testAsyncTableTransaction() throws RequestException, JSONObjectAdapterException {
		String tableId = "syn3889291";
		String jobId = "99994";
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		client.getAsynchJobResults(AsynchType.TableTransaction, jobId, request, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/" + tableId + TABLE_TRANSACTION + ASYNC_GET + jobId;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
	}
	
	@Test
	public void testStartAsynchJob() throws RequestException, JSONObjectAdapterException {
		QueryBundleRequest request = new QueryBundleRequest();
		request.setEntityId("syn292");
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		
		client.startAsynchJob(AsynchType.TableQuery, request, mockAsyncCallback);
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/syn292" + TABLE_QUERY + ASYNC_START;
		verify(mockRequestBuilder).configure(POST, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(WebConstants.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
	}
	
	@Test
	public void testAsyncGetTableQueryJobResults() throws RequestException, JSONObjectAdapterException {
		String entityId = "syn387453";
		String jobId = "99992";
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		QueryBundleRequest request = new QueryBundleRequest();
		request.setEntityId(entityId);
		client.getAsynchJobResults(AsynchType.TableQuery, jobId, request, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/" + entityId + TABLE_QUERY + ASYNC_GET + jobId;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
		
		// response status code is OK, but if we request a query result but it responds with a job status, then a ResultNotReadyException should be thrown
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.PROCESSING);
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		jobStatus.writeToJSONObject(adapter);
		
		verify(mockRequestBuilder).sendRequest(eq((String)null), requestCallbackCaptor.capture());
		RequestCallback requestCallback = requestCallbackCaptor.getValue();
		when(mockResponse.getStatusCode()).thenReturn(SC_ACCEPTED);
		when(mockResponse.getText()).thenReturn(adapter.toJSONString());
		requestCallback.onResponseReceived(mockRequest, mockResponse);
		
		verify(mockAsyncCallback).onFailure(throwableCaptor.capture());
		Throwable th = throwableCaptor.getValue();
		assertTrue(th instanceof ResultNotReadyException);
		assertEquals(jobStatus, ((ResultNotReadyException)th).getStatus());
	}
	
	@Test
	public void testAsyncBulkFileDownload() throws RequestException, JSONObjectAdapterException {
		String jobId = "99993";
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		BulkFileDownloadRequest request = new BulkFileDownloadRequest();
		request.setRequestedFiles(new ArrayList<>());
		client.getAsynchJobResults(AsynchType.BulkFileDownload, jobId, request, mockAsyncCallback);
		
		//verify url and method
		String url = FILE_ENDPOINT + FILE_BULK + ASYNC_GET + jobId;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
	}
	
	@Test
	public void testAsyncCSVDownload() throws RequestException, JSONObjectAdapterException {
		String tableId = "syn388378";
		String jobId = "99994";
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentUserSessionToken()).thenReturn(USER_SESSION_TOKEN);
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setEntityId(tableId);
		client.getAsynchJobResults(AsynchType.TableCSVDownload, jobId, request, mockAsyncCallback);
		
		//verify url and method
		String url = REPO_ENDPOINT + ENTITY + "/" + tableId + TABLE_DOWNLOAD_CSV + ASYNC_GET + jobId;
		verify(mockRequestBuilder).configure(GET, url);
		verify(mockRequestBuilder).setHeader(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		verify(mockRequestBuilder).setHeader(SESSION_TOKEN_HEADER, USER_SESSION_TOKEN);
	}
	
	public void testGetTableQueryJobResultsReady() throws RequestException, JSONObjectAdapterException, ResultNotReadyException {
		// test round trip (response really is a QueryResultBundle, which should be re-created from the json)
		QueryResultBundle resultBundle = new QueryResultBundle();
		resultBundle.setQueryCount(42L);
		JSONObjectAdapter adapter = jsonObjectAdapter.createNew();
		resultBundle.writeToJSONObject(adapter);
		
		QueryResultBundle newResultBundleInstance = (QueryResultBundle) synapseJsFactory.newInstance(OBJECT_TYPE.AsyncResponse, adapter);
		
		assertEquals(resultBundle, newResultBundleInstance);
	}
}
