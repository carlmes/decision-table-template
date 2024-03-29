package test.kjar.build;

import org.appformer.maven.support.AFReleaseId;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.examples.decisiontable.Driver;
import org.drools.examples.decisiontable.Policy;
import org.drools.examples.decisiontable.RuleName;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.scanner.KieMavenRepository;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * JUnit test to figure out how to get the Decision Table Template working with ExternalSpreadsheetCompiler
 * 
 * Useful resources:
 *    https://docs.jboss.org/drools/release/latestFinal/drools-docs/html_single/#_rule_templates
 *    https://access.redhat.com/solutions/2048283
 *    https://stackoverflow.com/a/46159667
 *    https://www.programcreek.com/java-api-examples/?code=IncQueryLabs/smarthome-cep-demonstrator/smarthome-cep-demonstrator-master/runtime/com.incquerylabs.smarthome.eventbus.ruleengine.drools/src/main/java/com/incquerylabs/smarthome/eventbus/ruleengine/drools/DroolsEventBusClient.java
 *    https://github.com/kiegroup/drools/blob/master/drools-examples/src/main/resources/META-INF/kmodule.xml#L77
 */

@FixMethodOrder( MethodSorters.NAME_ASCENDING )
public class TestDecisionTableBuild {

	private KieServices kieServices = KieServices.Factory.get();
	private AFReleaseId releaseId = kieServices.newReleaseId( "test.kjar.build", "test-kjar-build-package", "1.0.0" );

	@Test
	public void buildKJarTest() {
		
		// "drools.trimCellsInDTable"
		// --------------------------
		// Requires Drools 7.18+
		//
		// https://docs.jboss.org/drools/release/7.22.0.Final/drools-docs/html_single/#_trimcellsindtableoption
		// https://github.com/kiegroup/drools/blob/master/drools-decisiontables/src/test/java/org/drools/decisiontable/SpreadsheetIntegrationExampleTest.java#L146
		
		System.setProperty( "drools.trimCellsInDTable", "false" );
		
		
		// Remove any previously installed artifact
		// ----------------------------------------
		
		KieMavenRepository.getKieMavenRepository().removeLocalArtifact( releaseId );
		

		// Define the resources
		// --------------------

		List<Resource> rulesResources = new ArrayList<Resource>();

		Resource example1 = ResourceFactory.newFileResource( "src/main/resources/other-rules/Example1.drl" );
		example1.setResourceType( ResourceType.determineResourceType( example1.getSourcePath() ) );
		example1.setTargetPath( "org/drools/examples/banking/" + new File( example1.getSourcePath() ).getName() );
		rulesResources.add( example1 );

		Resource example2 = ResourceFactory.newFileResource( new File( "src/main/resources/other-rules/Example2.drl" ) );
		example2.setResourceType( ResourceType.determineResourceType( example2.getSourcePath() ) );
		example2.setTargetPath( "org/drools/examples/banking/" + new File( example2.getSourcePath() ).getName() );
		rulesResources.add( example2 );

		Resource baseDRT = ResourceFactory.newFileResource( new File( "src/main/resources/decision-table-template/BasePricing.drt" ) );
		baseDRT.setResourceType( ResourceType.determineResourceType( baseDRT.getSourcePath() ) );
		baseDRT.setTargetPath( "org/drools/examples/decisiontable/" + new File( baseDRT.getSourcePath() ).getName() );
		rulesResources.add( baseDRT );

		Resource promoDRT = ResourceFactory.newFileResource( new File( "src/main/resources/decision-table-template/PromotionalPricing.drt" ) );
		promoDRT.setResourceType( ResourceType.determineResourceType( promoDRT.getSourcePath() ) );
		promoDRT.setTargetPath( "org/drools/examples/decisiontable/" + new File( promoDRT.getSourcePath() ).getName() );
		rulesResources.add( promoDRT );

		Resource xlsxResource = ResourceFactory.newFileResource( new File( "src/main/resources/decision-table-template/ExamplePolicyPricingTemplateData.xls" ) );
		xlsxResource.setResourceType( ResourceType.DTABLE );
		xlsxResource.setTargetPath( "org/drools/examples/decisiontable-template/" + new File( xlsxResource.getSourcePath() ).getName() );
		DecisionTableConfiguration xlsxDecisionTableConfiguration = KnowledgeBuilderFactory.newDecisionTableConfiguration();
		xlsxDecisionTableConfiguration.setInputType( DecisionTableInputType.XLS );
		xlsxDecisionTableConfiguration.addRuleTemplateConfiguration( baseDRT, 3, 3 );
		xlsxDecisionTableConfiguration.addRuleTemplateConfiguration( promoDRT, 18, 3 );
		xlsxDecisionTableConfiguration.setTrimCell( false );		
		xlsxResource.setConfiguration( xlsxDecisionTableConfiguration );

		rulesResources.add( xlsxResource );

		Resource trimcellDRT = ResourceFactory.newFileResource( new File( "src/main/resources/trimCell-template-test/trim-cell-test2.drt" ) );
		trimcellDRT.setResourceType( ResourceType.determineResourceType( trimcellDRT.getSourcePath() ) );
		trimcellDRT.setTargetPath( "org/drools/examples/decisiontable/" + new File( trimcellDRT.getSourcePath() ).getName() );
		rulesResources.add( trimcellDRT );

		Resource trimcellXLSX = ResourceFactory.newFileResource( new File( "src/main/resources/trimCell-template-test/trim-cell-test.xlsx" ) );
		trimcellXLSX.setResourceType( ResourceType.DTABLE );
		trimcellXLSX.setTargetPath( "org/drools/examples/decisiontable-template/" + new File( trimcellXLSX.getSourcePath() ).getName() );

		DecisionTableConfiguration trimCellConfig = KnowledgeBuilderFactory.newDecisionTableConfiguration();
		trimCellConfig.setInputType( DecisionTableInputType.XLS );
		trimCellConfig.addRuleTemplateConfiguration( baseDRT, 2, 1 );

		/*
		 * Note: This method of configuration is NOT saved in the KJar.
		 *       ------------------------------------------------------
		 *       
		 * This is because the properties will not be written, since the toProperties() and 
		 * fromProperties() methods don't include "templates" and "trimCell" attributes.
		 * 
		 * The "templates" can be defined using kmodule.xml configuration, but not "trimCell"
		 * 
		 * See: https://github.com/kiegroup/drools/pull/2377
		 * 
		 * Fix was NOT accepted, so cannot use this?
		 */
		trimCellConfig.setTrimCell( false );	

		trimcellXLSX.setConfiguration( trimCellConfig );		
		rulesResources.add( trimcellXLSX );


		
		// Generate the pom.xml
		// --------------------

		String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n"
			+ "  <modelVersion>4.0.0</modelVersion>\n" + "  <groupId>" + releaseId.getGroupId()
			+ "</groupId>\n" + "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" + "  <version>"
			+ releaseId.getVersion() + "</version>\n" + "</project>";

		System.out.println( "\nPOM contents: \n-------------\n" + pom + "\n" );	


		// Generate the kmodule.xml
		// ------------------------

		KieModuleModel kproj = kieServices.newKieModuleModel();
		kproj.newKieBaseModel( "KB" ).setDefault( true ) 
			.addRuleTemplate( "org/drools/examples/decisiontable-template/ExamplePolicyPricingTemplateData.xls", 
			                  "org/drools/examples/decisiontable/BasePricing.drt", 3, 3 ) 
			.addRuleTemplate( "org/drools/examples/decisiontable-template/ExamplePolicyPricingTemplateData.xls", 
			                  "org/drools/examples/decisiontable/PromotionalPricing.drt", 18, 3 ) 
			.addRuleTemplate( "org/drools/examples/decisiontable-template/trim-cell-test.xlsx", 
			                  "org/drools/examples/decisiontable/trim-cell-test2.drt", 2, 1 ) 
			.newKieSessionModel( "KS" ).setDefault( true );

		System.out.println( "\nKMODULE contents: \n-----------------\n" + kproj.toXML() + "\n" );


		// Create the KJar using KieFileSystem
		// -----------------------------------

		KieFileSystem kfs = kieServices.newKieFileSystem();

		for ( Resource resource : rulesResources ) {
			System.out.println( "Writing resource: " + resource.getTargetPath() );
			kfs.write( resource );
		}

		System.out.println( "Writing resource: " + "kmodule.xml" );
		kfs.writeKModuleXML( kproj.toXML() );

		System.out.println( "Writing resource: " + "pom.xml" );
		kfs.writePomXML( pom );


		// Build it
		// --------

		System.out.println( "\n" );
		System.out.println( "Building the new KJar" );
		System.out.println( "---------------------" );
		
		KieBuilder kieBuilder = kieServices.newKieBuilder( kfs ).buildAll();

		List<Message> errors = kieBuilder.getResults().getMessages( Message.Level.ERROR );
		if ( errors.size() > 0 ) {
			for ( Message errorMessage : errors ) {
				System.out.println( errorMessage.toString() );
			}
			throw new IllegalArgumentException( "Could not parse knowledge." );
		}


		// Install it
		// ----------

		InternalKieModule internalKieModule = (InternalKieModule) kieBuilder.getKieModule();
		KieMavenRepository.getKieMavenRepository().installArtifact( releaseId, internalKieModule.getBytes(), pom.getBytes() );

		System.out.println( "\nInstalled artifact: " + releaseId );

		assert( true );
	}

	@Test
	public void runSampleDecisionTableRules() {
		ReleaseId kieReleaseId = kieServices.newReleaseId( releaseId.getGroupId(), releaseId.getArtifactId(), releaseId.getVersion() );		
		System.out.println( "\nRunning rules using version: " + kieReleaseId + "\n");

		kieServices.newEnvironment();
		KieContainer kieContainer = kieServices.newKieContainer( kieReleaseId );
		KieSession kieSession = kieContainer.newKieSession();

		// Create some test data
		Driver driver = new Driver();
		driver.setName( "Tim" );
		driver.setAge( 19 );
		driver.setPriorClaims( 0 );
		driver.setLocationRiskProfile( "LOW" );
		kieSession.insert(driver);

		Policy policy = new Policy();
		policy.setType( "COMPREHENSIVE" );
		kieSession.insert(policy);

		kieSession.fireAllRules();

		// Verify that the example templates (BasePrice and PromotionalPricing) ran successfully
		System.out.println( "BASE PRICE IS: " + policy.getBasePrice() );
		assertEquals( policy.getBasePrice(), 150 );

		System.out.println( "DISCOUNT IS: " + policy.getDiscountPercent() );  
		assertEquals( policy.getDiscountPercent(), 1 );

		kieSession.dispose();
	}

	@Test
	public void runTrimCellTests() {
		ReleaseId kieReleaseId = kieServices.newReleaseId( releaseId.getGroupId(), releaseId.getArtifactId(), releaseId.getVersion() );
		System.out.println( "\nRunning rules using version: " + kieReleaseId + "\n");

		kieServices.newEnvironment();
		KieContainer kieContainer = kieServices.newKieContainer( kieReleaseId );
		KieSession kieSession = kieContainer.newKieSession();

		kieSession.fireAllRules();

		// Verify that the trimCell template test ran successfully
		Collection<? extends Object> ruleNames = kieSession.getObjects( new ClassObjectFilter( RuleName.class ) );
		System.out.println( "Trim Cell test generated " + ruleNames.size() + " rule names" );

		assertEquals( ruleNames.size(), 2 );	// Two of the three rules have empty (blank) cells

		kieSession.dispose();
	}

}
