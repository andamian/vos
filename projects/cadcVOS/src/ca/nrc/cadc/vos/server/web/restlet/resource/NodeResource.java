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

package ca.nrc.cadc.vos.server.web.restlet.resource;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeFault;
import ca.nrc.cadc.vos.NodeParsingException;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.server.auth.PrivilegedReadAuthorizationExceptionAction;
import ca.nrc.cadc.vos.server.auth.PrivilegedWriteAuthorizationExceptionAction;
import ca.nrc.cadc.vos.server.auth.VOSpaceAuthorizer;
import ca.nrc.cadc.vos.server.SearchNode;
import ca.nrc.cadc.vos.server.web.representation.NodeErrorRepresentation;
import ca.nrc.cadc.vos.server.web.representation.NodeInputRepresentation;
import ca.nrc.cadc.vos.server.web.restlet.action.CreateNodeAction;
import ca.nrc.cadc.vos.server.web.restlet.action.DeleteNodeAction;
import ca.nrc.cadc.vos.server.web.restlet.action.GetNodeAction;
import ca.nrc.cadc.vos.server.web.restlet.action.NodeAction;
import ca.nrc.cadc.vos.server.web.restlet.action.UpdatePropertiesAction;

/**
 * Handles HTTP requests for Node resources.
 * 
 * @author majorbNodeNotFoundException
 *
 */
public class NodeResource extends BaseResource
{
    private static Logger log = Logger.getLogger(NodeResource.class);
    
    private static final String CERTIFICATE_REQUEST_ATTRIBUTE_NAME = "org.restlet.https.clientCertificates";
    
    private String path;
    private Node node;
    private String ownerDN;
    private NodeFault nodeFault;
    private String faultMessage;
    private VOSURI vosURI;
    
    
    /**
     * Called after object instantiation.
     */
    public void doInit()
    {
        log.debug("Enter NodeResource.doInit(): " + getMethod());

        // Create a subject for authentication
        Set<Principal> principals = getPrincipals(getRequest());
        Subject subject = null;
        if (principals != null && principals.size() > 0) {
            Set<Object> emptyCredentials = new HashSet<Object>();
            subject = new Subject(true, principals, emptyCredentials, emptyCredentials);
        }
        
        log.debug(principals.size() + " principals found in request.");
        
        try
        {
            Set<Method> allowedMethods = new CopyOnWriteArraySet<Method>();
            allowedMethods.add(Method.GET);
            allowedMethods.add(Method.PUT);
            allowedMethods.add(Method.DELETE);
            allowedMethods.add(Method.POST);
            setAllowedMethods(allowedMethods);

            path = (String) getRequest().getAttributes().get("nodePath");  
            log.debug("path = " + path);
            
            if (path == null || path.trim().length() == 0)
            {
                throw new IllegalArgumentException("No node path information provided.");
            }
            
            vosURI = new VOSURI(getVosUriPrefix() + "/" + path);
            
            // Get the node for the operation, either from the persistent layer,
            // or from the client supplied xml
            Node clientNode = null;
            if (getMethod().equals(Method.GET) || getMethod().equals(Method.DELETE))
            {
                clientNode = new SearchNode(vosURI);
            }
            else if (getMethod().equals(Method.PUT) || getMethod().equals(Method.POST))
            {
                Representation xml = getRequestEntity();
                NodeInputRepresentation nodeInputRepresentation =
                    new NodeInputRepresentation(xml, path);
                clientNode = nodeInputRepresentation.getNode();
            }
            else
            {
                throw new UnsupportedOperationException("Method not supported: " + getMethod());
            }
            
            log.debug("Client node is: " + clientNode);
            
            // perform the authorization check
            VOSpaceAuthorizer voSpaceAuthorizer = new VOSpaceAuthorizer();
            voSpaceAuthorizer.setNodePersistence(getNodePersistence());
            try
            {
                if (getMethod().equals(Method.GET))
                {
                    PrivilegedReadAuthorizationExceptionAction readAuthorization =
                        new PrivilegedReadAuthorizationExceptionAction(voSpaceAuthorizer, clientNode);
                    node = (Node) Subject.doAs(subject, readAuthorization);
                }
                else if (getMethod().equals(Method.PUT))
                {
                    PrivilegedWriteAuthorizationExceptionAction writeAuthorization =
                        new PrivilegedWriteAuthorizationExceptionAction(voSpaceAuthorizer, clientNode.getParent());
                    ContainerNode parent = (ContainerNode) Subject.doAs(subject, writeAuthorization);
                    node = clientNode;
                    node.setParent(parent);
                    // set the owner on the node to be the distinguished name
                    // contained in the client certificate(s)
                    node.setOwner(ownerDN);
                }
                else 
                {
                    // (DELETE or POST)
                    PrivilegedWriteAuthorizationExceptionAction writeAuthorization =
                        new PrivilegedWriteAuthorizationExceptionAction(voSpaceAuthorizer, clientNode);
                    node = (Node) Subject.doAs(subject, writeAuthorization);
                    node.setProperties(clientNode.getProperties());
                }
            }
            catch (PrivilegedActionException e)
            {
                log.info("Authentication failed: " + e.getCause().getMessage());
                throw e.getCause();
            }
            
            node.setUri(vosURI);
            
            log.debug("doInit() retrived node: " + node);
        }
        catch (FileNotFoundException e)
        {
            faultMessage = vosURI.toString();
            log.debug("Could not find node with path: " + path, e);
            nodeFault = NodeFault.NodeNotFound;
        }
        catch (URISyntaxException e)
        {
            faultMessage = "URI not well formed: " + vosURI;
            log.debug(faultMessage, e);
            nodeFault = NodeFault.InvalidURI;
        }
        catch (AccessControlException e)
        {
            faultMessage = "Access Denied: " + e.getMessage();
            log.debug(faultMessage, e);
            nodeFault = NodeFault.PermissionDenied;
        }
        catch (NodeParsingException e)
        {
            faultMessage = "Node XML not well formed: " + e.getMessage();
            log.debug(faultMessage, e);
            nodeFault = NodeFault.InvalidToken;
        }
        catch (UnsupportedOperationException e)
        {
            faultMessage = "Not supported: " + e.getMessage();
            log.debug(faultMessage, e);
            nodeFault = NodeFault.NotSupported;
        }
        catch (IllegalArgumentException e)
        {
            faultMessage = "Bad input: " + e.getMessage();
            log.debug(faultMessage, e);
            nodeFault = NodeFault.BadRequest;
        }
        catch (Throwable t)
        {
            faultMessage = "Internal Error:" + t.getMessage();
            log.debug(faultMessage, t);
            nodeFault = NodeFault.InternalFault;
        }
    }
    
    /**
     * Performs the specified node action, first checking for errors, and
     * catching any errors that may happen.
     * @param action The action to perform.
     * @return A representation of the output or of any error that happens.
     */
    public Representation performNodeAction(NodeAction action)
    {
        log.debug("Enter NodeResource.performNodeAction()");
        try
        {
            if (nodeFault != null)
            {
                setStatus(nodeFault.getStatus());
                if (faultMessage == null)
                {
                    return new NodeErrorRepresentation(nodeFault);
                }
                else
                {
                    return new NodeErrorRepresentation(nodeFault, faultMessage);
                }
            }

            return action.perform(this);
        }
        catch (Throwable t)
        {
            log.debug(t);
            setStatus(NodeFault.InternalFault.getStatus());
            return new NodeErrorRepresentation(NodeFault.InternalFault, t.getMessage());
        }
    }
    
    /**
     * HTTP GET
     */
    @Get("xml")
    public Representation represent()
    {
        log.debug("Enter NodeResource.represent()");
        return performNodeAction(new GetNodeAction());
    }
    
    /**
     * HTTP PUT
     */
    @Put("xml")
    public Representation store()
    {   
        log.debug("Enter NodeResource.store()");
        return performNodeAction(new CreateNodeAction());
    }
    
    /**
     * HTTP POST
     */
    @Post("xml")
    public Representation accept()
    {
        log.debug("Enter NodeResource.accept()");
        return performNodeAction(new UpdatePropertiesAction());
    }
    
    /**
     * HTTP DELETE
     */
    @Delete
    public Representation remove()
    {
        log.debug("Enter NodeResource.remove()");
        return performNodeAction(new DeleteNodeAction());
    }
    
    /**
     * Using the restlet request object, get all the
     * certificate authentication principals.
     * @param request The restlet request object.
     * @return A set of principals found in the restlet request.
     */
    @SuppressWarnings("unchecked")
    private Set<Principal> getPrincipals(Request request) {
        
        Set<Principal> principals = new HashSet<Principal>();
        // look for X509 certificates
        Map<String, Object> requestAttributes = request.getAttributes();
        if (requestAttributes.containsKey(CERTIFICATE_REQUEST_ATTRIBUTE_NAME))
        {
            final Collection<X509Certificate> clientCertificates =
                (Collection<X509Certificate>)requestAttributes.get(CERTIFICATE_REQUEST_ATTRIBUTE_NAME);
            
            if ((clientCertificates != null) && (!clientCertificates.isEmpty()))
            {
                for (final X509Certificate cert : clientCertificates)
                {
                    principals.add(cert.getSubjectX500Principal());
                    if (ownerDN == null)
                    {
                        ownerDN = cert.getSubjectX500Principal().getName();
                    }
                }
            }
        }
        
        return principals;
        
    }
    
    public Node getNode()
    {
        return node;
    }
    
    public VOSURI getVosURI()
    {
        return vosURI;
    }
    
    public String getPath()
    {
        return path;
    }
    
}
