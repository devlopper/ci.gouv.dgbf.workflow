package ci.gouv.dgbf.system.workflow.server.persistence.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.cyk.utility.helper.AbstractHelper;
import org.cyk.utility.string.StringHelper;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.UserGroupCallback;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import ci.gouv.dgbf.system.workflow.server.persistence.api.WorkflowPersistence;
import ci.gouv.dgbf.system.workflow.server.persistence.entities.Workflow;
import ci.gouv.dgbf.system.workflow.server.persistence.entities.bpmn.Bpmn;

@Singleton
public class JbpmHelperImpl extends AbstractHelper implements JbpmHelper, Serializable {
	private static final long serialVersionUID = 1L;

	private RuntimeEnvironment runtimeEnvironment;
	private UserGroupCallback userGroupCallback;
	private RuntimeManager runtimeManager;
	private RuntimeEngine runtimeEngine;
	private String processesMavenRepositoryFolder;
	
	@Override
	public RuntimeEnvironment getRuntimeEnvironment() {
		if(runtimeEnvironment == null)
			buildRuntimeEnvironment();
		return runtimeEnvironment;
	}
	
	@Override
	public JbpmHelper setRuntimeEnvironment(RuntimeEnvironment runtimeEnvironment) {
		this.runtimeEnvironment = runtimeEnvironment;
		return this;
	}
	
	@Override
	public UserGroupCallback getUserGroupCallback() {
		return userGroupCallback;
	}
	
	@Override
	public JbpmHelper setUserGroupCallback(UserGroupCallback userGroupCallback) {
		this.userGroupCallback = userGroupCallback;
		return this;
	}
	
	@Override
	public RuntimeManager getRuntimeManager() {
		if(runtimeManager == null)
			runtimeManager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(getRuntimeEnvironment());
		return runtimeManager;
	}
	
	@Override
	public JbpmHelper setRuntimeManager(RuntimeManager runtimeManager) {
		this.runtimeManager = runtimeManager;
		return this;
	}
	
	@Override
	public RuntimeEngine getRuntimeEngine() {
		return runtimeEngine = getRuntimeManager().getRuntimeEngine(ProcessInstanceIdContext.get());
	}
	
	@Override
	public String getProcessesMavenRepositoryFolder() {
		return processesMavenRepositoryFolder;
	}
	
	@Override
	public JbpmHelper setProcessesMavenRepositoryFolder(String processesMavenRepositoryFolder) {
		this.processesMavenRepositoryFolder = processesMavenRepositoryFolder;
		return this;
	}
	
	@Override
	public JbpmHelper buildRuntimeEnvironment() {
		System.out.println("Runtime environment build started.");
		//KieServices kieServices = KieServices.Factory.get();
		//KieContainer kieContainer = kieServices.newKieContainer( kieServices.getRepository().getDefaultReleaseId() );
		KieBase kieBase = null;
		
		UserGroupCallback userGroupCallback = getUserGroupCallback();
		RuntimeEnvironmentBuilder runtimeEnvironmentBuilder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder()
				.entityManagerFactory(__inject__(EntityManagerFactory.class))
				.knowledgeBase(kieBase).userGroupCallback(userGroupCallback);
		
		// Get all workflow from database
		Collection<Workflow> workflows = __inject__(WorkflowPersistence.class).readMany();
		if(workflows!=null)
			for(Workflow index : workflows)
				__addProcess__(runtimeEnvironmentBuilder,index.getModel());
		
		// Get all workflow from maven repository
		String processesMavenRepositoryFolder  = getProcessesMavenRepositoryFolder();
		if(__inject__(StringHelper.class).isNotBlank(processesMavenRepositoryFolder)) {
			System.out.println("Processes maven repository folder is "+processesMavenRepositoryFolder);
			for(File indexFile : FileUtils.listFiles(new File(processesMavenRepositoryFolder), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
				if(indexFile.getAbsolutePath().endsWith(".jar")) {
					try {
						ZipFile zipFile = new ZipFile(indexFile.getAbsoluteFile());
						//zipFile.stream().map(ZipEntry::getName).forEach(System.out::println);
						Enumeration zipEntries = zipFile.entries();
					    while (zipEntries.hasMoreElements()) {
					    	ZipEntry entry = ((ZipEntry) zipEntries.nextElement());
					        String fileName = entry.getName();
					        if(fileName.endsWith(".bpmn2")) {
					        	String content = IOUtils.toString(zipFile.getInputStream(entry), "UTF-8");
					        	__addProcess__(runtimeEnvironmentBuilder, content);
					        }
					    }
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//resources.add(StringUtils.substringAfter(indexFile.getPath(), javaResourceFolder));
			}
		}
		
		setRuntimeEnvironment(runtimeEnvironmentBuilder.get());
		System.out.println("Runtime environment build done.");
		return this;
	}
	
	protected void __addProcess__(RuntimeEnvironmentBuilder runtimeEnvironmentBuilder,String string) {
		Bpmn bpmn = Bpmn.__executeWithContent__(string);
		runtimeEnvironmentBuilder.addAsset(ResourceFactory.newByteArrayResource(string.getBytes()), ResourceType.BPMN2);
		System.out.println("\tProcess <<"+bpmn.getProcess().getId()+">> added");
	}
	
	@Override
	public JbpmHelper destroyRuntimeEnvironment() {
		if(runtimeEnvironment!=null) {
			System.out.println("Runtime environment destroy started.");
			if(runtimeManager!=null) {
				if(runtimeEngine!=null) {
					runtimeManager.disposeRuntimeEngine(runtimeEngine);
					runtimeEngine = null;
				}
				runtimeManager.close();
				runtimeManager = null;
			}
			
			runtimeEnvironment.close();
			runtimeEnvironment = null;
			System.out.println("Runtime environment destroy done.");
		}
		return this;
	}
	
	
}
