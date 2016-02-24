package org.sagebionetworks.web.unitclient.widget.entity.browse;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils.SelectedHandler;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.widget.entity.browse.EntityFinder;
import org.sagebionetworks.web.client.widget.entity.browse.EntityFinderArea;
import org.sagebionetworks.web.client.widget.entity.browse.EntityFinderView;
import org.sagebionetworks.web.shared.PaginatedResults;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EntityFinderTest {

	EntityFinderView mockView;
	SynapseClientAsync mockSynapseClient;
	AdapterFactory adapterFactory;
	GlobalApplicationState mockGlobalApplicationState;
	AuthenticationController mockAuthenticationController;

	EntityFinder entityFinder;	
	@Mock
	ClientCache mockClientCache;
	
	@Before
	public void before() throws JSONObjectAdapterException {
		MockitoAnnotations.initMocks(this);
		mockView = mock(EntityFinderView.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		adapterFactory = new AdapterFactoryImpl();
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		
		entityFinder = new EntityFinder(mockView, mockSynapseClient, mockGlobalApplicationState, mockAuthenticationController, mockClientCache);
		verify(mockView).setPresenter(entityFinder);
		reset(mockView);
		when(mockView.isShowing()).thenReturn(false);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadEntity() throws Exception {
		String name = "name";
		String id = "syn456";
		Entity entity = new Folder();
		entity.setId(id);
		entity.setName(name);
		AsyncMockStubber.callSuccessWith(entity).when(mockSynapseClient).getEntity(eq(id), any(AsyncCallback.class));		
		AsyncCallback<Entity> mockCallback = mock(AsyncCallback.class);
		
		entityFinder.lookupEntity(id, mockCallback);
		
		verify(mockCallback).onSuccess(entity);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadEntityFail() throws Exception {
		String name = "name";
		String id = "syn456";
		Entity entity = new Folder();
		entity.setId(id);
		entity.setName(name);
		AsyncCallback<Entity> mockCallback = mock(AsyncCallback.class);

		// 404
		AsyncMockStubber.callFailureWith(new NotFoundException()).when(mockSynapseClient).getEntity(eq(id), any(AsyncCallback.class));			
		entityFinder.lookupEntity(id, mockCallback);		
		verify(mockCallback).onFailure(any(Throwable.class));
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_NOT_FOUND);		
		reset(mockCallback);
		reset(mockView);
		
		// 403
		AsyncMockStubber.callFailureWith(new ForbiddenException()).when(mockSynapseClient).getEntity(eq(id), any(AsyncCallback.class));			
		entityFinder.lookupEntity(id, mockCallback);
		verify(mockCallback).onFailure(any(Throwable.class));
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_FAILURE_PRIVLEDGES);
		reset(mockCallback);
		reset(mockView);

		// other
		AsyncMockStubber.callFailureWith(new Exception()).when(mockSynapseClient).getEntity(eq(id), any(AsyncCallback.class));		
		entityFinder.lookupEntity(id, mockCallback);
		verify(mockCallback).onFailure(any(Throwable.class));
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_GENERIC);
		reset(mockCallback);
		reset(mockView);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadVersions() throws Exception {
		String id = "syn456";
		String versionsJson = "versions";
		PaginatedResults<VersionInfo> paginated = new PaginatedResults<VersionInfo>();		
		List<VersionInfo> results = new ArrayList<VersionInfo>();
		paginated.setResults(results);
		AsyncMockStubber.callSuccessWith(paginated).when(mockSynapseClient).getEntityVersions(eq(id), anyInt(), anyInt(), any(AsyncCallback.class));		
		AsyncCallback<Entity> mockCallback = mock(AsyncCallback.class);	
		
		entityFinder.loadVersions(id);
		
		verify(mockView).setVersions(results);
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadVersionsFail() throws Exception {
		String id = "syn456";
		PaginatedResults<VersionInfo> paginated = new PaginatedResults<VersionInfo>();		
		List<VersionInfo> results = new ArrayList<VersionInfo>();
		paginated.setResults(results);
		AsyncMockStubber.callFailureWith(new Exception()).when(mockSynapseClient).getEntityVersions(eq(id), anyInt(), anyInt(), any(AsyncCallback.class));		
		
		entityFinder.loadVersions(id);
		
		verify(mockView).showErrorMessage(DisplayConstants.UNABLE_TO_LOAD_VERSIONS);
	}
	
	@Test
	public void testSelectionHandler() throws Exception {
		SelectedHandler mockHandler = mock(SelectedHandler.class);
		entityFinder.configure(true, mockHandler);
		
		//then the view calls okClicked if the user has clicked ok in the entity finder
		
		//no selection
		entityFinder.okClicked();
		//verify the error
		verify(mockView).showErrorMessage(DisplayConstants.PLEASE_MAKE_SELECTION);
		
		//now with selection
		Reference mockReference = mock(Reference.class);
		when(mockReference.getTargetId()).thenReturn("syn99");
		//the view usually sets the selected entity in the presenter
		entityFinder.setSelectedEntity(mockReference);
		entityFinder.okClicked();
		verify(mockHandler).onSelected(mockReference);
	}	
	
	@Test
	public void testShowDefaultArea() {
		entityFinder.show();
		verify(mockView).clear();
		verify(mockView).setBrowseAreaVisible();
		verify(mockView).show();
	}
	@Test
	public void testShowBrowseArea() {
		when(mockClientCache.get(EntityFinder.ENTITY_FINDER_AREA_KEY)).thenReturn(EntityFinderArea.BROWSE.toString());
		entityFinder.show();
		verify(mockView).clear();
		verify(mockView).setBrowseAreaVisible();
		verify(mockView).show();
	}
	@Test
	public void testShowSearchArea() {
		when(mockClientCache.get(EntityFinder.ENTITY_FINDER_AREA_KEY)).thenReturn(EntityFinderArea.SEARCH.toString());
		entityFinder.show();
		verify(mockView).clear();
		verify(mockView).setSearchAreaVisible();
		verify(mockView).show();
	}
	@Test
	public void testShowSynIdArea() {
		when(mockClientCache.get(EntityFinder.ENTITY_FINDER_AREA_KEY)).thenReturn(EntityFinderArea.SYNAPSE_ID.toString());
		entityFinder.show();
		verify(mockView).clear();
		verify(mockView).setSynapseIdAreaVisible();
		verify(mockView).show();
	}
	@Test
	public void testHideNoArea() {
		when(mockView.getCurrentArea()).thenReturn(null);
		entityFinder.hide();
		verify(mockClientCache, never()).put(anyString(), anyString());
	}
	@Test
	public void testHideSearchArea() {
		when(mockView.getCurrentArea()).thenReturn(EntityFinderArea.SEARCH);
		entityFinder.hide();
		verify(mockClientCache).put(EntityFinder.ENTITY_FINDER_AREA_KEY, EntityFinderArea.SEARCH.toString());
	}
}


