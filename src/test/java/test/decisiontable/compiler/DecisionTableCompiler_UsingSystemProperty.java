package test.decisiontable.compiler;

/* Decision Table Compiler and Builder
* ===================================
*
* See the inline file name configuration for the .xlsx and .drt input files
*/

import java.io.InputStream;

import org.drools.decisiontable.ExternalSpreadsheetCompiler;
import org.drools.decisiontable.InputType;
import org.drools.template.parser.DefaultTemplateContainer;
import org.drools.template.parser.TemplateContainer;
import org.drools.template.parser.TemplateDataListener;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderError;
import org.kie.internal.builder.KnowledgeBuilderErrors;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

public class DecisionTableCompiler_UsingSystemProperty {

	public static void main(String[] args) throws Exception {
				
		System.out.println( "==============" );
		System.out.println( " Compiling... " );
		System.out.println( "==============" );

		InputStream xlsStream = ResourceFactory.newClassPathResource( "trimCell-template-test/trim-cell-test.xlsx" ).getInputStream();

		InputStream drtStream = ResourceFactory.newClassPathResource( "trimCell-template-test/trim-cell-test.drt" ).getInputStream();

		// "drools.trimCellsInDTable"
		// --------------------------
		// Requires Drools 7.18+
		//
		// https://docs.jboss.org/drools/release/7.22.0.Final/drools-docs/html_single/#_trimcellsindtableoption
		// https://github.com/kiegroup/drools/blob/master/drools-decisiontables/src/test/java/org/drools/decisiontable/SpreadsheetIntegrationExampleTest.java#L146
		
		System.setProperty( "drools.trimCellsInDTable", "false" );
		
		// The following constructor should use the system property for default behaviour
		// But it won't work in the 7.18 code base:
		// https://github.com/kiegroup/drools/blob/7.18.0.t040/drools-templates/src/main/java/org/drools/template/parser/DefaultTemplateContainer.java#L50

		TemplateContainer drtContainer = new DefaultTemplateContainer( drtStream );
		
		try {
			drtStream.close();
		} catch (final Exception e) {
			System.err.println( "WARNING: Wasn't able to correctly close stream for decision table." + e.getMessage() );
		}

		ExternalSpreadsheetCompiler converter = new ExternalSpreadsheetCompiler();
		final String drl = converter.compile( xlsStream, InputType.XLS, new TemplateDataListener( 2, 1, drtContainer ) );

		System.out.println( drl );

		System.out.println( "==============" );
		System.out.println( " Building...  " );
		System.out.println( "==============" );

		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add( ResourceFactory.newByteArrayResource( drl.getBytes() ), ResourceType.DRL );

		KnowledgeBuilderErrors errors = kbuilder.getErrors();
		if ( errors.size() > 0 ) {
			for ( KnowledgeBuilderError error : errors ) {
				System.err.println( error );
			}
			throw new IllegalArgumentException( "Could not parse knowledge." );
		}
		else {
			System.out.println( "" );
			System.out.println( "BUILD OK - NO ERRORS" );
			System.out.println( "--------------------" );
		}

	}
}
