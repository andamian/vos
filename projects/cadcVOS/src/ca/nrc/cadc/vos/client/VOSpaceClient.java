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

package ca.nrc.cadc.vos.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.Direction;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeParsingException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.NodeReader;
import ca.nrc.cadc.vos.NodeWriter;
import ca.nrc.cadc.vos.Protocol;
import ca.nrc.cadc.vos.Transfer;
import ca.nrc.cadc.vos.TransferParsingException;
import ca.nrc.cadc.vos.TransferReader;
import ca.nrc.cadc.vos.TransferWriter;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.View;

/**
 * VOSpace client library. This implementation 
 * 
 * @author zhangsa
 */
public class VOSpaceClient
{
    private static Logger log = Logger.getLogger(VOSpaceClient.class);
    
    public static final String CR = System.getProperty("line.separator"); // OS independant new line

    // TODO: get this from capabilites obtained via registry lookup
    public static final String VOSPACE_SYNC_TRANSFER_ENDPOINT = "/synctrans";
    public static final String VOSPACE_ASYNC_TRANSFER_ENDPOINT = "/transfers";
    public static final String VOSPACE_ASYNC_NODEPROPS_ENDPONT = "/nodeprops";
    public static final String VOSPACE_NODE_ENDPOINT = "/nodes";

    protected String baseUrl;
    boolean schemaValidation;
    private SSLSocketFactory sslSocketFactory;


    /**
     * Constructor. XML Schema validation is enabled by default.
     * 
     * @param baseUrl
     */
    public VOSpaceClient(String baseUrl)
    {
        this(baseUrl, true);
    }

    /**
     * Constructor. XML schema validation may be disabled, in which case the client
     * is likely to fail in horrible ways (e.g. NullPointerException) if it receives
     * invalid VOSpace (node or transfer) or UWS (job) documents. However, performance
     * may be improved.
     * 
     * @param baseUrl
     * @param enableSchemaValidation
     */
    public VOSpaceClient(String baseUrl, boolean enableSchemaValidation)
    {
        this.baseUrl = baseUrl;
        this.schemaValidation = enableSchemaValidation;
    }

    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory)
    {
        this.sslSocketFactory = sslSocketFactory;
    }

    // temp hack to share SSL with ClientTransfer
    public SSLSocketFactory getSslSocketFactory()
    {
        initHTTPS(null);
        return sslSocketFactory;
    }

    /**
     * Create the specified node. If the parent (container) nodes do not exist, they
     * will also be created.
     * 
     * @param node
     * @return the created node
     */
    public Node createNode(Node node)
    {
        return this.createNode(node, true);
    }

    /**
     * Create the specified node. If the parent (container) nodes do not exist, they
     * will also be created.
     * 
     * @param node
     * @param checkForDuplicate If true, throw duplicate node exception if node
     * already exists.
     * @return the created node
     */
    private Node createNode(Node node, boolean checkForDuplicate)
    {
        int responseCode;
        Node rtnNode = null;

        try
        {
            VOSURI parentURI = node.getUri().getParentURI();
            ContainerNode parent = null;
            if (parentURI == null)
                throw new RuntimeException("parent (root node) not found and cannot create: " + node.getUri());
            try
            {
                // check for existence--get the node with minimal content.  get the target child
                // if we need to check for duplicates.
                Node p = null;
                if (checkForDuplicate)
                    p = this.getNode(parentURI.getPath(), "detail=min&limit=1&uri=" + NetUtil.encode(node.getUri().toString()));
                else
                    p = this.getNode(parentURI.getPath(), "detail=min&limit=0");
                
                log.debug("found parent: " + parentURI);
                if (p instanceof ContainerNode)
                    parent = (ContainerNode) p;
                else
                    throw new IllegalArgumentException("cannot create a child, parent is a " + p.getClass().getSimpleName());
            }
            catch(NodeNotFoundException ex)
            {
                // if parent does not exist, just create it!!
                log.info("creating parent: " + parentURI);
                ContainerNode cn = new ContainerNode(parentURI);
                parent = (ContainerNode) createNode(cn, false);
            }

            // check if target already exists: also could fail like this below due to race condition
            if (checkForDuplicate)
                for (Node n : parent.getNodes())
                    if (n.getName().equals(node.getName()))
                        throw new IllegalArgumentException("DuplicateNode: " + node.getUri().getURIObject().toASCIIString());

            URL url = new URL(this.baseUrl + "/nodes" + node.getUri().getPath());
            log.debug("createNode(), URL=" + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection)
            {
                HttpsURLConnection sslConn = (HttpsURLConnection) connection;
                initHTTPS(sslConn);
            }
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            NodeWriter nodeWriter = new NodeWriter();
            nodeWriter.write(node, out);
            out.close();

            String responseMessage = connection.getResponseMessage();
            String errorBody = NetUtil.getErrorBody(connection);
            if (StringUtil.hasText(errorBody))
            {
                responseMessage += ": " + errorBody;
            }
            responseCode = connection.getResponseCode();
            log.debug("createNode(), response code: " + responseCode);
            log.debug("createNode(), response message: " + responseMessage);
            
            switch (responseCode)
            {
                case 200: // valid re spec
                case 201: // valid re previous spec
                    InputStream in = connection.getInputStream();
                    NodeReader nodeReader = new NodeReader(schemaValidation);
                    rtnNode = nodeReader.read(in);
                    in.close();
                    log.debug("createNode, created node: " + rtnNode);
                    break;

                case 500:
                    // The service SHALL throw a HTTP 500 status code including an InternalFault fault in the entity body
                    // if the operation fails

                    // If a parent node in the URI path does not exist
                    // then the service MUST throw a HTTP 500 status code including a ContainerNotFound fault in the entity body.

                    // If a parent node in the URI path is a LinkNode,
                    // the service MUST throw a HTTP 500 status code including a LinkFound fault in the entity body.
                    throw new RuntimeException(responseMessage);
                case 409:
                    // The service SHALL throw a HTTP 409 status code including a DuplicateNode fault in the entity body
                    // if a Node already exists with the same URI
                    throw new IllegalArgumentException(responseMessage);
                case 400:
                    // The service SHALL throw a HTTP 400 status code including an InvalidURI fault in the entity body
                    // if the requested URI is invalid
                    // The service SHALL throw a HTTP 400 status code including a TypeNotSupported fault in the entity body
                    // if the type specified in xsi:type is not supported
                    throw new IllegalArgumentException(responseMessage);
                case 401:
                    // The service SHALL throw a HTTP 401 status code including PermissionDenied fault in the entity body
                    // if the user does not have permissions to perform the operation
                    String msg = responseMessage;
                    if (msg == null)
                        msg = "permission denied";
                    throw new AccessControlException(msg);

                case 404:
                    // handle server response when parent (container) does not exist
                    throw new IllegalArgumentException(responseMessage);
                default:
                    throw new RuntimeException("unexpected failure mode: " + responseMessage + "(" + responseCode + ")");
            }
        }
        catch (IOException e)
        {
            log.debug("failed to create node", e);
            throw new IllegalStateException("failed to create node", e);
        }
        catch (NodeParsingException e)
        {
            throw new IllegalStateException("failed to create node", e);
        }
        return rtnNode;
    }

    /**
     * Get Node.
     *  
     * @param path      The path to the Node.
     * @return          The Node instance.
     * @throws NodeNotFoundException when the requested node does not exist on the server
     */
    public Node getNode(String path)
        throws NodeNotFoundException
    {
        return getNode(path, null);
    }

    /**
     * Get Node.
     *
     * @param path      The path to the Node.
     * @param query     Optional query string
     * @return          The Node instance.
     * @throws NodeNotFoundException when the requested node does not exist on the server
     */
    public Node getNode(String path, String query)
        throws NodeNotFoundException
    {
        if ( path.length() > 0 && !path.startsWith("/")) // length 0 is root: no /
            path = "/" + path; // must be absolute
        if (query != null)
            path += "?" + query;

		int responseCode;
        final Node rtnNode;

        try
        {
        
            URL url = new URL(this.baseUrl + "/nodes" + path);
            log.debug("getNode(), URL=" + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection)
            {
                HttpsURLConnection sslConn = (HttpsURLConnection) connection;
                initHTTPS(sslConn);
            }
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);

            String responseMessage = connection.getResponseMessage();
            String errorBody = NetUtil.getErrorBody(connection);
            if (StringUtil.hasText(errorBody))
            {
                responseMessage += ": " + errorBody;
            }
            responseCode = connection.getResponseCode();

            switch (responseCode)
            {
                case 200: // TODO: check content-type for XML
                    // grab service response body
                    InputStream in = connection.getInputStream();
                    NodeReader nodeReader = new NodeReader(schemaValidation);
                    rtnNode = nodeReader.read(in);
                    in.close();
                    log.debug("getNode, returned node: " + rtnNode);
                    break;
                case 500:
                    // The service SHALL throw a HTTP 500 status code including an InternalFault fault in the entity-body if the operation fails
                    throw new RuntimeException("service failed: " + responseMessage);
                case 401:
                    // The service SHALL throw a HTTP 401 status code including a PermissionDenied fault in the entity-body if the user does not have permissions to perform the operation
                    String msg = responseMessage;
                    if (msg == null)
                        msg = "permission denied";
                    throw new AccessControlException(msg);
                case 404:
                    // The service SHALL throw a HTTP 404 status code including a NodeNotFound fault in the entity-body if the target Node does not exist
                    throw new NodeNotFoundException("not found: " + path);
                default:
                    log.error(responseMessage + ". HTTP Code: " + responseCode);
                    throw new IllegalArgumentException("Error returned.  HTTP Response Code: " + responseCode);
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("failed to get node", ex);
        }
        catch (NodeParsingException e)
        {
            throw new IllegalStateException("failed to get node", e);
        }
        return rtnNode;
    }

    /**
     * Add --recursiveMode option to command line, used by set only, and if set
     * setNode uses a different recursiveMode endpoint.
     */
    /**
     * @param node
     * @return
     */
    public Node setNode(Node node)
    {
        Node rtnNode = null;
        try
        {
            URL url = new URL(this.baseUrl + "/nodes" + node.getUri().getPath());
            log.debug("setNode: " + VOSClientUtil.xmlString(node));
            log.debug("setNode: " + url);
            
            NodeWriter nodeWriter = new NodeWriter();
            StringBuilder nodeXML = new StringBuilder();
            nodeWriter.write(node, nodeXML);
            HttpPost httpPost = new HttpPost(url, nodeXML.toString(), null, false);
            httpPost.run();
  
            checkFailureClean(httpPost.getThrowable());
            
            String responseBody = httpPost.getResponseBody();
            NodeReader nodeReader = new NodeReader();
            rtnNode = nodeReader.read(responseBody);
            
        }
        catch (IOException e)
        {
            throw new IllegalStateException("failed to set node", e);
        }
        catch (NodeParsingException e)
        {
            throw new IllegalStateException("failed to set node", e);
        }
        return rtnNode;
    }
    
    // create an async transfer job
    public ClientRecursiveSetNode setNodeRecursive(Node node)
    {
        try
        {
            String asyncNodePropsUrl = this.baseUrl + VOSPACE_ASYNC_NODEPROPS_ENDPONT;
            NodeWriter nodeWriter = new NodeWriter();
            Writer stringWriter = new StringWriter();
            nodeWriter.write(node, stringWriter);
            URL postUrl = new URL(asyncNodePropsUrl);
            
            HttpPost httpPost = new HttpPost(postUrl, stringWriter.toString(), "text/xml", false);
            httpPost.run();
            checkFailureClean(httpPost.getThrowable());
            
            URL jobUrl = httpPost.getRedirectURL();
            log.debug("Job URL is: " + jobUrl.toString());

            // we have only created the job, not run it
            return new ClientRecursiveSetNode(jobUrl, node, schemaValidation);
        }
        catch (MalformedURLException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Negotiate a transfer. The argument transfer specifies the target URI, the
     * direction, the proposed protocols, and an optional view.
     *
     * @param trans
     * @return a negotiated transfer
     */
    public ClientTransfer createTransfer(Transfer trans)
    {
        if (Direction.pushToVoSpace.equals(trans.getDirection()))
            return createTransferSync(trans);

        if (Direction.pullFromVoSpace.equals(trans.getDirection()))
            return createTransferSync(trans);
        
        return createTransferASync(trans);
    }

    public List<NodeProperty> getProperties()
    {
        throw new UnsupportedOperationException("Feature under construction.");
    }

    public List<Protocol> getProtocols()
    {
        throw new UnsupportedOperationException("Feature under construction.");
    }

    public List<View> getViews()
    {
        throw new UnsupportedOperationException("Feature under construction.");
    }

    public void deleteNode(String path)
    {
        if ( path.length() > 0 && !path.startsWith("/")) // length 0 is root: no /
            path = "/" + path; // must be absolute

        int responseCode;
        try
        {
            URL url = new URL(this.baseUrl + "/nodes" + path);
            log.debug(url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection)
            {
                HttpsURLConnection sslConn = (HttpsURLConnection) connection;
                initHTTPS(sslConn);
            }
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);

            String responseMessage = connection.getResponseMessage();
            String errorBody = NetUtil.getErrorBody(connection);
            if (StringUtil.hasText(errorBody))
            {
                responseMessage += ": " + errorBody;
            }
            
            responseCode = connection.getResponseCode();
            switch (responseCode)
            {
            case 200: // successful
                break;

            case 500:
                // The service SHALL throw a HTTP 500 status code including an InternalFault fault in the entity-body 
                // if the operation fails
                //
                // If a parent node in the URI path does not exist then 
                // the service MUST throw a HTTP 500 status code including a ContainerNotFound fault in the entity-body
                //
                // If a parent node in the URI path is a LinkNode, 
                // the service MUST throw a HTTP 500 status code including a LinkFound fault in the entity-body.
                throw new RuntimeException(responseMessage);
            case 401:
                /* The service SHALL throw a HTTP 401 status code including a PermissionDenied fault in the entity-body 
                 * if the user does not have permissions to perform the operation
                 */
                String msg = responseMessage;
                if (msg == null)
                    msg = "permission denied";
                throw new AccessControlException(msg);
            case 404:
                /*
                 * The service SHALL throw a HTTP 404 status code including a NodeNotFound fault in the entity-body 
                 * if the target node does not exist
                 * 
                 * If the target node in the URI path does not exist, 
                 * the service MUST throw a HTTP 404 status code including a NodeNotFound fault in the entity-body. 
                 */
                throw new IllegalArgumentException(responseMessage);
            default:
                log.error(responseMessage + ". HTTP Code: " + responseCode);
                throw new RuntimeException("unexpected failure mode: " + responseMessage + "(" + responseCode + ")");
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("failed to delete node", ex);
        }
    }

    public String getBaseURL()
    {
        return baseUrl;
    }

    //public void setBaseUrl(String baseUrl)
    //{
    //    this.baseUrl = baseUrl;
    //}

    // create an async transfer job
    private ClientTransfer createTransferASync(Transfer transfer)
    {
        try
        {
            String asyncTransUrl = this.baseUrl + VOSPACE_ASYNC_TRANSFER_ENDPOINT;
            TransferWriter transferWriter = new TransferWriter();
            Writer stringWriter = new StringWriter();
            transferWriter.write(transfer, stringWriter);
            URL postUrl = new URL(asyncTransUrl);
            
            HttpPost httpPost = new HttpPost(postUrl, stringWriter.toString(), "text/xml", false);
            httpPost.run();
            checkFailureClean(httpPost.getThrowable());
            
            URL jobUrl = httpPost.getRedirectURL();
            log.debug("Job URL is: " + jobUrl.toString());

            // we have only created the job, not run it
            return new ClientTransfer(jobUrl, transfer, schemaValidation);
        }
        catch (MalformedURLException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
    }

    // create a transfer using the sync transfers job resource
    private ClientTransfer createTransferSync(Transfer transfer)
    {
        try
        {                      
            // POST the Job and get the redirect location.
            TransferWriter writer = new TransferWriter();
            StringWriter sw = new StringWriter();
            writer.write(transfer, sw);
            
            URL postUrl = new URL(this.baseUrl + VOSPACE_SYNC_TRANSFER_ENDPOINT);
            HttpPost httpPost = new HttpPost(postUrl, sw.toString(), "text/xml", false);
            httpPost.run();
            checkFailureClean(httpPost.getThrowable());
            
            URL redirectURL = httpPost.getRedirectURL();
            log.debug("POST: transfer jobURL: " + redirectURL);
            if (redirectURL == null)
            {
                throw new RuntimeException("Redirect not received from UWS.");
            }

            log.debug("GET - opening connection: " + redirectURL.toString());
            // follow the redirect to run the job
            HttpURLConnection conn = (HttpURLConnection) redirectURL.openConnection();
            if (conn instanceof HttpsURLConnection)
            {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                initHTTPS(sslConn);
            }
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            int code = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();
            String errorBody = NetUtil.getErrorBody(conn);
            if (StringUtil.hasText(errorBody))
            {
                responseMessage += ": " + errorBody;
            }
            if (code != 200)
            {
                throw new RuntimeException("failed to read transfer description (" + code + "): " + responseMessage);
            }
            
            TransferReader txfReader = new TransferReader(schemaValidation);
            log.debug("GET - reading content: " + redirectURL);
            Transfer trans = txfReader.read(conn.getInputStream());
            log.debug("GET - done: " + redirectURL);
            log.debug("negotiated transfer: " + trans);

            URL jobURL = extractJobURL(this.baseUrl, redirectURL);
            return new ClientTransfer(jobURL, trans, schemaValidation);
        }
        catch (MalformedURLException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log.debug("failed to create transfer", e);
            throw new RuntimeException(e);
        }
        /*
        catch (JDOMException e) // from JobReader
        {
            log.debug("got bad job XML from service", e);
            throw new RuntimeException(e);
        }
        catch (ParseException e) // from JobReader
        {
            log.debug("got bad job XML from service", e);
            throw new RuntimeException(e);
        }
        */
        catch (TransferParsingException e)
        {
            log.debug("got invalid XML from service", e);
            throw new RuntimeException(e);
        }
    }
    
    private void checkFailureClean(Throwable failure)
    {
        try
        {
            checkFailure(failure);
        }
        catch (IOException ioe)
        {
            throw new IllegalArgumentException(ioe);
        }
        catch (NodeNotFoundException nf)
        {
            throw new IllegalArgumentException(nf);
        }
    }
    
    private void checkFailure(Throwable failure)
            throws NodeNotFoundException, IOException, RuntimeException
    {
        if (failure != null)
        {
            failure.printStackTrace();
            if (failure instanceof RuntimeException)
            {
                throw (RuntimeException) failure;
            }
            if (failure instanceof FileNotFoundException)
            {
                throw new NodeNotFoundException("not found.", failure);
            }
            throw new IllegalStateException(failure);
        }
    }

    // determine the jobURL from the service base URL and the URL to
    // transfer details... makes assumptions about paths structure that
    // can be simplified once we comply to spec
    private URL extractJobURL(String baseURL, URL transferDetailsURL)
        throws MalformedURLException
    {
        //log.warn("baseURL: " + baseURL);
        URL u = new URL(baseURL);
        String bp = u.getPath();
        //log.warn("bp: " + bp);
        String tu = transferDetailsURL.toExternalForm();
        //log.warn("tu: " + tu);
        int i = tu.indexOf(bp);
        String jp = tu.substring(i + bp.length() + 1); // strip /
        //log.warn("jp: " + jp);
        String[] parts = jp.split("/");
        // part[0] is the joblist
        // part[1] is the jobID
        // part[2-] is either run (current impl) or results/transferDetails (spec)
        String jobList = parts[0];
        String jobID = parts[1];
        //log.warn("jobList: " + jobList);
        //log.warn("jobID: " + jobID);
        return new URL(baseURL + "/" + jobList + "/" + jobID);
    }

    private void initHTTPS(HttpsURLConnection sslConn)
    {
        if (sslSocketFactory == null) // lazy init
        {
            log.debug("initHTTPS: lazy init");
            AccessControlContext ac = AccessController.getContext();
            Subject s = Subject.getSubject(ac);
            this.sslSocketFactory = SSLUtil.getSocketFactory(s);
        }
        if (sslSocketFactory != null && sslConn != null)
        {
            log.debug("setting SSLSocketFactory on " + sslConn.getClass().getName());
            sslConn.setSSLSocketFactory(sslSocketFactory);
        }
    }
}
