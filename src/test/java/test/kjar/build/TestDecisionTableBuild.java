package test.kjar.build;

import org.appformer.maven.support.AFReleaseId;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TestDecisionTableBuild {
	
	private KieServices kieServices = KieServices.Factory.get();
	
	@Test
	public void testSomeLibraryMethod() {

		// Build the KJar

		List<Resource> rulesResources = new ArrayList<Resource>();

		Resource example1 = ResourceFactory.newFileResource( new File( "src/main/resources/other-rules/Example1.drl" ) );
		example1.setResourceType( ResourceType.determineResourceType( example1.getSourcePath() ) );
		example1.setTargetPath( example1.getResourceType().getDefaultPath() + "/" + new File( example1.getSourcePath() ).getName() );
		rulesResources.add( example1 );

		Resource example2 = ResourceFactory.newFileResource( new File( "src/main/resources/other-rules/Example2.drl" ) );
		example2.setResourceType( ResourceType.determineResourceType( example2.getSourcePath() ) );
		example2.setTargetPath( example2.getResourceType().getDefaultPath() + "/" + new File( example2.getSourcePath() ).getName() );
		rulesResources.add( example2 );

		Resource drtResource = ResourceFactory.newFileResource( new File( "src/main/resources/decision-table-template/BasePricing.drt" ) );
		drtResource.setResourceType( ResourceType.DRT );
		drtResource.setTargetPath( drtResource.getResourceType().getDefaultPath() + "/" + new File( drtResource.getSourcePath() ).getName() );
		rulesResources.add( drtResource );

		/*
		 * Debug notes: 
		 * 
		 * The following code block attempts to add the decision table resource using a template, but the buildAll()
		 * throws a NullPointerException. When examining the stack trace shows that the code reaches this method:
		 * 
		 * KnowledgeBuilderImpl.decisionTableToPackageDescr() 
		 * 
		 * https://github.com/kiegroup/drools/blob/7.14.0.Final/drools-compiler/src/main/java/org/drools/compiler/builder/impl/KnowledgeBuilderImpl.java#L371
		 * 
		 * It's supposed to call the DecisionTableFactory.loadFromInputStreamWithTemplates(resource, dtableConfiguration) on line 379
		 * But instead it's reaching the DecisionTableFactory.loadFromResource(resource, dtableConfiguration) method on line 397
		 * 
		 * Aargh, it seems that the logic at the top of this function that tests for the existence of Decision Table Configuration is not working.	
		 * 
		 */
		
		Resource xlsxResource = ResourceFactory.newFileResource( new File( "src/main/resources/decision-table-template/ExamplePolicyPricingTemplateData.xls" ) );
		xlsxResource.setResourceType( ResourceType.DTABLE );
		xlsxResource.setTargetPath( xlsxResource.getResourceType().getDefaultPath() + "/" + new File( xlsxResource.getSourcePath() ).getName() );
		DecisionTableConfiguration xlsxDecisionTableConfiguration = KnowledgeBuilderFactory.newDecisionTableConfiguration();
		xlsxDecisionTableConfiguration.setInputType( DecisionTableInputType.XLS );
		xlsxDecisionTableConfiguration.addRuleTemplateConfiguration( drtResource, 2, 1 );
		xlsxResource.setConfiguration( xlsxDecisionTableConfiguration );
		rulesResources.add( xlsxResource );
		
		/*
		 * Note: if you comment out the block of code above, and run it, then it works fine. 
		 * Rule 01 has a basic System.out.println() that will print to the console to verify rules running ok.
		 */
		

		// Start the build process
		// -----------------------

		AFReleaseId releaseId = kieServices.newReleaseId( "test.kjar.build", "test-kjar-build-package", "1.0.0" );
		log.info( "Building the KJar: {}", releaseId );

		// Generate the Pom file contents
		// ------------------------------

		String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n"
			+ "  <modelVersion>4.0.0</modelVersion>\n" + "\n" + "  <groupId>" + releaseId.getGroupId()
			+ "</groupId>\n" + "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" + "  <version>"
			+ releaseId.getVersion() + "</version>\n" + "\n" + "</project>";

		// Create the KJar
		// ---------------

		log.debug( "Defining KJAR kbase and kie session model." );
		KieModuleModel kproj = kieServices.newKieModuleModel();

		KieFileSystem kfs = kieServices.newKieFileSystem();

		log.debug( "Writing resource: {}", "kmodule.xml" );
		kfs.writeKModuleXML( kproj.toXML() );

		log.debug( "Writing resource: {}", "pom.xml" );
		kfs.writePomXML( pom );

		for ( Resource resource : rulesResources ) {
			log.debug( "Writing resource: {}", resource.getTargetPath() );
			kfs.write( resource );
		}

		// Build it
		// --------
		
		log.info( "Building the new KJar" );
		log.info( "---------------------" );

		KieBuilder kieBuilder = kieServices.newKieBuilder( kfs ).buildAll();
		
	    List<Message> errors = kieBuilder.getResults().getMessages( Message.Level.ERROR );
		if ( errors.size() > 0 ) {
			for ( Message errorMessage : errors ) {
				log.error( errorMessage.toString() );
			}
			throw new IllegalArgumentException( "Could not parse knowledge." );
		}

		InternalKieModule internalKieModule = (InternalKieModule) kieBuilder.getKieModule();

		// Run it
		// ------

		log.info( "Running the rules" );
		log.info( "-----------------" );
		
	    KieContainer kieContainer = kieServices.newKieContainer( internalKieModule.getReleaseId() );

        KieBase kieBase = kieContainer.getKieBase();
        KieSession kieSession = kieContainer.newKieSession();

        log.info( "Execution result: {}", kieSession.fireAllRules() );

		log.info( "TEST COMPLETED" );
		assert ( true );
	}
}
