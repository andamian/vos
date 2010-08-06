/**
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2010.                            (c) 2010.
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
 ************************************************************************
 */
package ca.nrc.cadc.vos.server.auth;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.Authorizer;
import ca.nrc.cadc.gms.client.GmsClient;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.server.NodePersistence;
import ca.nrc.cadc.vos.server.SearchNode;
import ca.nrc.cadc.vos.server.util.NodeStackListener;
import ca.nrc.cadc.vos.server.util.NodeUtil;


/**
 * Authorization implementation for VO Space. 
 */
public class VOSpaceAuthorizer implements Authorizer
{
    protected static final Logger LOG = Logger.getLogger(VOSpaceAuthorizer.class);
    
    private NodePersistence nodePersistence;

    public VOSpaceAuthorizer()
    {
    }

    /**
     * Obtain the Read Permission for the given URI.
     *
     * @param uri       The URI to check.
     * @return          The Read Permission objectual representation, such as
     *                  a Group, or User.
     * @throws AccessControlException If the user does not have read permission
     * @throws FileNotFoundException If the node could not be found
     */
    public Object getReadPermission(final URI uri)
            throws AccessControlException, FileNotFoundException
    {
        try
        {
            Node searchNode = new SearchNode(new VOSURI(uri));
            return getReadPermission(searchNode);
            
        } catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("URI not well formed.");
        }
    }
    
    /**
     * Obtain the Read Permission for the given Node.
     *
     * @param node      The Node to check.
     * @return          The persistent version of the target node.
     * @throws AccessControlException If the user does not have read permission
     * @throws FileNotFoundException If the node could not be found
     */
    public Object getReadPermission(final Node node)
            throws AccessControlException, FileNotFoundException
    {        
        NodeStackListener readPermissionAuthorizer = new NodeStackListener()
        {
            @Override
            public void nodeVisited(Node node, int parentLevel)
            {
                
                // Check if the node is public
                if (node.isPublic())
                {
                    // Node is open for public reading
                    return;
                }
                
                AccessControlContext acContext = AccessController.getContext();
                Subject subject = Subject.getSubject(acContext);
                
                // return true if this is the owner of the node or if a member
                // of the groupRead property
                if (subject != null) {
                    
                    X500Principal nodeOwner = new X500Principal(node.getOwner());
                    
                    Set<Principal> principals = subject.getPrincipals();
                    for (Principal principal : principals)
                    {
                        NodeProperty groupRead = node.findProperty(VOS.PROPERTY_URI_GROUPREAD);
                        
                        if (LOG.isDebugEnabled())
                        {
                            String principalString = principal == null ? "null" : principal.getName();
                            String groupReadString = groupRead == null ? "null" : groupRead.getPropertyValue();
                            LOG.debug(String.format("Checking read permission on node \"%s\" (owner=\"%s\",groupRead=\"%s\") where user=\"%s\"",
                                    node.getName(), node.getOwner(), groupReadString, principalString));
                        }
                        
                        // Check for ownership
                        if (AuthenticationUtil.equals(nodeOwner, principal))
                        {
                            // User is the owner
                            return;
                        }
                        
                        // Check for group membership
                        if (principal != null && groupRead != null && groupRead.getPropertyValue() != null)
                        {
                            if (hasMembership(groupRead.getPropertyValue(), principal))
                            {
                                // User is a member
                                return;
                            }
                        }
                    }
                }
                
                throw new AccessControlException("Read permission denied.");
            }};
        try
        {
            Node persistentNode = NodeUtil.iterateStack(node, readPermissionAuthorizer, getNodePersistence());
            if (persistentNode == null)
            {
                throw new AccessControlException("Read permission on root denied.");
            }
            return persistentNode;
        }
        catch (NodeNotFoundException e)
        {
            throw new FileNotFoundException(node.getPath());
        }
    }

    /**
     * Obtain the Write Permission for the given URI.
     *
     * @param uri       The URI to check.
     * @return          The Write Permission objectual representation, such as
     *                  a Group, or User.
     * @throws AccessControlException If the user does not have write permission
     * @throws FileNotFoundException If the node could not be found
     */
    public Object getWritePermission(final URI uri)
            throws AccessControlException, FileNotFoundException
    {
        try
        {
            Node searchNode = new SearchNode(new VOSURI(uri));
            return getWritePermission(searchNode);
            
        } catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("URI not well formed.");
        }
    }

    /**
     * Obtain the Write Permission for the given Node.
     *
     * @param uri       The Node to check.
     * @return          The persistent version of the target node.
     * @throws AccessControlException If the user does not have write permission
     * @throws FileNotFoundException If the node could not be found
     */
    public Object getWritePermission(final Node node)
            throws AccessControlException, FileNotFoundException
    {
        NodeStackListener writePermissionAuthorizer = new NodeStackListener()
        {
            @Override
            public void nodeVisited(Node node, int parentLevel)
            {
                AccessControlContext acContext = AccessController.getContext();
                Subject subject = Subject.getSubject(acContext);
                
                // return true if this is the owner of the node
                if (subject != null) {
                    
                    X500Principal nodeOwner = new X500Principal(node.getOwner());
                    
                    Set<Principal> principals = subject.getPrincipals();
                    for (Principal principal : principals)
                    {   
                        
                        NodeProperty groupWrite = node.findProperty(VOS.PROPERTY_URI_GROUPWRITE);
                        
                        if (LOG.isDebugEnabled())
                        {
                            String principalString = principal == null ? "null" : principal.getName();
                            String groupWriteString = groupWrite == null ? "null" : groupWrite.getPropertyValue();
                            LOG.debug(String.format("Checking write permission on node (owner=\"%s\",groupWrite=\"%s\") where user=\"%s\"",
                                    node.getOwner(), groupWriteString, principalString));
                        }
                        
                        // Check for ownership
                        if (AuthenticationUtil.equals(nodeOwner, principal))
                        {
                            // User is the owner
                            return;
                        }
                        
                        // Check for group membership
                        if (principal != null && groupWrite != null && groupWrite.getPropertyValue() != null)
                        {
                            if (hasMembership(groupWrite.getPropertyValue(), principal))
                            {
                                // User is a member
                                return;
                            }
                        }
                    }
                }
                throw new AccessControlException("Write permission denied.");
                
            }};
        try
        {
            Node persistentNode = NodeUtil.iterateStack(node, writePermissionAuthorizer, getNodePersistence());
            if (persistentNode == null)
            {
                throw new AccessControlException("Write permission to root denied.");
            }
            return persistentNode;
        }
        catch (NodeNotFoundException e)
        {
            throw new FileNotFoundException(node.getPath());
        }
    }
    
    /**
     * Given the groupURI, determine if the user identified by 'principal'
     * has membership.
     * @param groupURI The group
     * @param principal The user identifier
     * @return True if the user is a member
     */
    private boolean hasMembership(String groupURI, Principal principal)
    {
        try
        {
            RegistryClient registryClient = new RegistryClient();
            URI guri = new URI(groupURI);
            URI serviceURI = new URI(guri.getScheme(), guri.getSchemeSpecificPart(), null); // drop fragment
            URL gmsBaseURL = registryClient.getServiceURL(serviceURI, VOS.GMS_PROTOCOL);

            LOG.debug("GmsClient baseURL: " + gmsBaseURL);
            if (gmsBaseURL == null)
            {
                LOG.warn("failed to find base URL for GMS service: " + serviceURI);
                return false;
            }
            GmsClient gms = new GmsClient(gmsBaseURL);

            boolean ret = gms.isMember(guri.getFragment(), principal.getName());
            LOG.debug("GmsClient.isMember(" + guri.getFragment() + "," + principal.getName() + " returned " + ret);
            return ret;
        }
        catch (MalformedURLException e)
        {
            LOG.error("GMSClient invalid URL", e);
            throw new IllegalStateException(e);
        }
        catch (URISyntaxException e)
        {
            LOG.warn("Invalid Group URI: " + groupURI, e);
            return false;
        }
    }

    /**
     * Node NodePersistence Getter.
     */
    public NodePersistence getNodePersistence()
    {
        return nodePersistence;
    }

    /**
     * Node NodePersistence Setter.
     */
    public void setNodePersistence(final NodePersistence nodePersistence)
    {
        this.nodePersistence = nodePersistence;
    }
    
}
