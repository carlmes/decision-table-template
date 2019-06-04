package test.kjar.build;

import java.io.FileInputStream;

import org.drools.javaparser.utils.Log;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class SimpleTest {

	private KieSession kieSession;
	
	@Test
	public void test() throws Exception {
		
		// Taken from: https://stackoverflow.com/a/24570355

	    KieServices kieServices = KieServices.Factory.get();
	    KieFileSystem kfs = kieServices.newKieFileSystem();

	    // for each DRL file, referenced by a plain old path name:
	    FileInputStream fis = new FileInputStream( "src/main/resources/other-rules/Example1.drl" );
	    kfs.write( "src/main/resources/Example1.drl",kieServices.getResources().newInputStreamResource( fis ) );

	    KieBuilder kieBuilder = kieServices.newKieBuilder( kfs ).buildAll();
	    Results results = kieBuilder.getResults();
	    if( results.hasMessages( Message.Level.ERROR ) ){
	        System.out.println( results.getMessages() );
	        throw new IllegalStateException( "### errors ###" );
	    }

	    KieContainer kieContainer =
	    kieServices.newKieContainer( kieServices.getRepository().getDefaultReleaseId() );

	    KieBase kieBase = kieContainer.getKieBase();
	    kieSession = kieContainer.newKieSession();

	    Log.info( "BUILD SUCCESSFUL" );
	    assert( true );
	}

}
