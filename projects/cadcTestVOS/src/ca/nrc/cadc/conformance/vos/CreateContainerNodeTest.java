/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 4 $
*
************************************************************************
*/

package ca.nrc.cadc.conformance.vos;

import org.junit.Ignore;
import org.junit.matchers.JUnitMatchers;
import org.junit.Assert;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.NodeReader;
import com.meterware.httpunit.WebResponse;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Test case for creating ContainerNodes.
 * 
 * @author jburke
 */
public class CreateContainerNodeTest extends VOSNodeTest
{
    private static Logger log = Logger.getLogger(CreateContainerNodeTest.class);

    public CreateContainerNodeTest()
    {
        super();
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void createContainerNode()
    {
        try
        {
            log.debug("createContainerNode");

            // Get a ContainerNode.
            ContainerNode node = getSampleContainerNode();

            // Add ContainerNode to the VOSpace.
            WebResponse response = put(node);
            assertEquals("PUT response code should be 200", 200, response.getResponseCode());

            // Get the response (an XML document)
            String xml = response.getText();
            log.debug("PUT XML:\r\n" + xml);

            // Create a DOM document from XML and validate against the VOSPace schema.
            NodeReader reader = new NodeReader();
            reader.read(xml);

            // Get the node from vospace
            response = get(node);
            assertEquals("GET response code should be 200", 200, response.getResponseCode());

            // Delete the node
            response = delete(node);
            assertEquals("DELETE response code should be 200", 200, response.getResponseCode());

            log.info("createContainerNode passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /**
     * The service SHALL throw a HTTP 409 status code including a DuplicateNode
     * fault in the entity body if a Node already exists with the same URI
     */
    @Test
    public void duplicateNodeFault()
    {
        try
        {
            log.debug("duplicateNodeFault");

            // Get a ContainerNode.
            ContainerNode node = getSampleContainerNode();

            // Add ContainerNode to the VOSpace.
            WebResponse response = put(node);
            assertEquals("PUT response code should be 200", 200, response.getResponseCode());

            // Get the response (an XML document)
            String xml = response.getText();
            log.debug("PUT XML:\r\n" + xml);

            // Create a DOM document from XML and validate against the VOSPace schema.
            NodeReader reader = new NodeReader();
            reader.read(xml);

            // Get the node from vospace
            response = get(node);
            assertEquals("GET response code should be 200", 200, response.getResponseCode());

            // Try and add the same ContainerNode to the VOSpace
            response = put(node);

            // Should get back a 409 status code.
            assertEquals("PUT response code should be 409 when creating a duplicate node", 409, response.getResponseCode());

            // Response entity body should contain 'DuplicateNode'
            assertThat(response.getText().trim(), JUnitMatchers.containsString("DuplicateNode"));
            
            // Delete the node
            response = delete(node);
            assertEquals("DELETE response code should be 200", 200, response.getResponseCode());

            log.info("duplicateNodeFault passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /**
     * The service SHALL throw a HTTP 400 status code including an InvalidURI
     * fault in the entity body if the requested URI is invalid.
     * 
     * Disabled because VOSURI will throw an InvalidURIException, 
     * so how to create an invalid URI for this test?
     */
    @Ignore("Currently unable to test")
    @Test
    public void invalidURIPathFault()
    {
        try
        {
            log.debug("invalidURIPathFault");

            // Create node with an invalid path, node A doesn't exist.
            ContainerNode nodeAB = getSampleContainerNode("/A/B");

            // Add ContainerNode to the VOSpace.
            WebResponse response = put(nodeAB);
            assertEquals("PUT response code should be 404 for a NodeNotFound", 404, response.getResponseCode());

            // Response entity body should contain 'InvalidURI'
            assertThat(response.getText().trim(), JUnitMatchers.containsString("InvalidURI"));
            
            // Check that the node wasn't created
            response = get(nodeAB);
            assertEquals("GET response code should be 404", 404, response.getResponseCode());

            log.info("invalidURIPathFault passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    /**
     * The service SHALL throw a HTTP 400 status code including a TypeNotSupported
     * fault in the entity body if the type specified in xsi:type is not supported
     */
    @Test
    public void typeNotSupportedFault()
    {
        try
        {
            log.debug("typeNotSupportedFault");

            // Get a ContainerNode.
            ContainerNode node = getSampleContainerNode();

            // Add ContainerNode to the VOSpace.
            WebResponse response = put(node, new InvalidTypeNodeWriter());
            assertEquals("PUT response code should be 400 for an invalid Node xsi:type", 400, response.getResponseCode());

            // Response entity body should contain 'TypeNotSupported'
            assertThat(response.getText().trim(), JUnitMatchers.containsString("TypeNotSupported"));
            
            // Check that the node wasn't created
            response = get(node);
            assertEquals("GET response code should be 404", 404, response.getResponseCode());

            log.info("typeNotSupportedFault passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /**
     * The service SHALL throw a HTTP 401 status code including PermissionDenied
     * fault in the entity body if the user does not have permissions to perform the operation
     */
    @Ignore("Currently unable to test")
    @Test
    public void permissionDeniedFault()
    {
        try
        {
            log.debug("permissionDeniedFault");

            // Get a ContainerNode.
            ContainerNode node = getSampleContainerNode();
            
            // Add ContainerNode to the VOSpace.
            WebResponse response = put(node);
            assertEquals("PUT response code should be 401", 401, response.getResponseCode());

            // Response message body should be 'PermissionDenied'
            assertEquals("Response message body should be 'PermissionDenied'", "PermissionDenied", response.getResponseMessage());

            // Check that the node wasn't created
            response = get(node);
            assertEquals("GET response code should be 404", 404, response.getResponseCode());

            log.info("permissionDeniedFault passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    /**
     * If a parent node in the URI path does not exist then the service MUST
     * throw a HTTP 500 status code including a ContainerNotFound fault in the entity body.
     * For example, given the URI path /a/b/c, the service must throw a HTTP 500
     * status code including a ContainerNotFound fault in the entity body if
     * either /a or /a/b do not exist.
     */
    @Test
    public void containerNotFoundFault()
    {
        try
        {
            log.debug("containerNotFoundFault");

            // Create a Node path /A/B
            ContainerNode nodeAB = getSampleContainerNode("/A/B");

            // Try and add the Node to the VOSpace.
            WebResponse response = put(nodeAB);
            assertEquals("PUT response code should be 500 for a ContainerNotFound", 500, response.getResponseCode());

            // Response entity body should contain 'ContainerNotFound'
            assertThat(response.getText().trim(), JUnitMatchers.containsString("ContainerNotFound"));        

            // Check that the node wasn't created
            response = get(nodeAB);
            assertEquals("GET response code should be 404", 404, response.getResponseCode());

            log.info("containerNotFoundFault passed.");
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception: " + unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

}
