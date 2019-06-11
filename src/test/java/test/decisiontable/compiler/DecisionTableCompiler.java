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

public class DecisionTableCompiler {

	public static void main(String[] args) throws Exception {

		System.out.println( "==============" );
		System.out.println( " Compiling... " );
		System.out.println( "==============" );

		InputStream xlsStream = ResourceFactory.newClassPathResource( "trimCell-template-test/trim-cell-test.xlsx" ).getInputStream();

		InputStream drtStream = ResourceFactory.newClassPathResource( "trimCell-template-test/trim-cell-test.drt" ).getInputStream();

		// See: https://access.redhat.com/solutions/2048283
		// The following "false" parameter in the DefaultTemplateContainer() constructor
		// disables the logic that will discard the line when data for @{xxx} is empty

		TemplateContainer drtContainer = new DefaultTemplateContainer( drtStream, false );

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
