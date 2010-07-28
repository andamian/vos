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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.ArgumentMap;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.vos.ClientTransfer;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.server.DataView;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.Protocol;
import ca.nrc.cadc.vos.Transfer;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.Transfer.Direction;
import ca.nrc.cadc.vos.View;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author zhangsa
 *
 */
public class Main
{
    public static final String CR = System.getProperty("line.separator"); // OS independant new line
    public static final String EL = " "; // empty line

    public static final String ARG_HELP = "help";
    public static final String ARG_VERBOSE = "verbose";
    public static final String ARG_DEBUG = "debug";
    public static final String ARG_H = "h";
    public static final String ARG_V = "v";
    public static final String ARG_D = "d";
    public static final String ARG_VIEW = "view";
    public static final String ARG_CREATE = "create";
    public static final String ARG_DELETE = "delete";
    public static final String ARG_SET = "set";
    public static final String ARG_COPY = "copy";
    public static final String ARG_TARGET = "target";
    public static final String ARG_GROUP_READ = "group-read";
    public static final String ARG_GROUP_WRITE = "group-write";
    public static final String ARG_PROP = "prop";
    public static final String ARG_SRC = "src";
    public static final String ARG_DEST = "dest";
    public static final String ARG_CONTENT_TYPE = "content-type";
    public static final String ARG_CONTENT_ENCODING = "content-encoding";
    public static final String ARG_CONTENT_MD5 = "content-md5";
    public static final String ARG_CERT = "cert";
    public static final String ARG_KEY = "key";

    public static final String VOS_PREFIX = "vos://";

    private static Logger log = Logger.getLogger(Main.class);
    private static final int INIT_STATUS = 1; // exit code for initialisation failure
    private static final int NET_STATUS = 2;  // exit code for client-server failures
    
    /**
     * Operations of VoSpace Client.
     * 
     * @author zhangsa
     *
     */
    public enum Operation {
        VIEW, CREATE, DELETE, SET, COPY
    };

    Operation operation;
    VOSURI target;
    List<NodeProperty> properties;
    URI source;
    URI destination;
    RegistryClient registryClient = new RegistryClient();
    Direction transferDirection = null;
    String baseUrl = null;
    VOSpaceClient client = null;
    File certFile = null;
    File keyFile = null;

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        ArgumentMap argMap = new ArgumentMap(args);
        Main command = new Main();

        if (argMap.isSet(ARG_HELP) || argMap.isSet(ARG_H))
        {
            usage();
            System.exit(0);
        }

        // Set debug mode
        if (argMap.isSet(ARG_DEBUG) || argMap.isSet(ARG_D))
        {
            Log4jInit.setLevel("ca.nrc.cadc.vos.client", Level.DEBUG);
        }
        else if (argMap.isSet(ARG_VERBOSE) || argMap.isSet(ARG_V))
        {
            Log4jInit.setLevel("ca.nrc.cadc.vos.client", Level.INFO);
        }
        else
            Log4jInit.setLevel("ca", Level.WARN);

        try
        {
            command.validateCommand(argMap);
            command.validateCommandArguments(argMap);
        }
        catch (IllegalArgumentException ex)
        {
            msg("illegal argument(s): " + ex.getMessage());
            msg("");
            usage();
            System.exit(INIT_STATUS);
        }

        try
        {
            command.init(argMap);
            command.run();
        }
        catch(Throwable t)
        {
            log.error("unexpected failure", t);
        }
        System.exit(0);
    }

    // encapsulate all messages to console here
    private static void msg(String s)
    {
        System.out.println(s);
    }

    private void run()
    {
        log.debug("run - START");
        if (this.operation.equals(Operation.CREATE))
        {
            doCreate();
        }
        else if (this.operation.equals(Operation.DELETE))
        {
            doDelete();
        }
        else if (this.operation.equals(Operation.VIEW))
        {
            doView();
        }
        else if (this.operation.equals(Operation.COPY))
        {
            doCopy();
        }
        else if (this.operation.equals(Operation.SET))
        {
            doSet();
        }
        log.debug("run - DONE");
    }

    private void doSet()
    {
        log.debug("doSet");
        try
        {
            log.debug("target.getPath()" + this.target.getPath());
            Node n = this.client.getNode(this.target.getPath());
            copyProperties(n);
            this.client.setNode(n);
            log.info("updated properties: " + this.target);
        }
        catch(NodeNotFoundException ex)
        {
            msg("not found: " + target);
            System.exit(NET_STATUS);
        }
        catch(Throwable t)
        {
            msg("failed to delete: " + target);
            if (t.getMessage() != null)
                msg("          reason: " + t.getMessage());
            else
                msg("          reason: " + t);
            System.exit(NET_STATUS);
        }
    }

    private void doDelete()
    {
        log.debug("doDelete");
        try
        {
            log.debug("target.getPath()" + this.target.getPath());
            this.client.deleteNode(this.target.getPath());
            log.info("deleted: " + this.target);
        }
        catch(Throwable t)
        {
            msg("failed to delete: " + target);
            if (t.getMessage() != null)
                msg("          reason: " + t.getMessage());
            else
                msg("          reason: " + t);
            System.exit(NET_STATUS);
        }
    }

    private void doCopy()
    {
        log.debug("doCopy");
        try
        {
            if (this.transferDirection.equals(Transfer.Direction.pushToVoSpace))
            {
                copyToVOSpace();
            }
            else if (this.transferDirection.equals(Transfer.Direction.pullFromVoSpace))
            {
                copyFromVOSpace();
            }
        }
        catch(Throwable t)
        {
            msg("failed to copy: " + source + " -> " + destination);
            if (t.getMessage() != null)
                msg("          reason: " + t.getMessage());
            else
                msg("          reason: " + t);
            t.printStackTrace();
            System.exit(NET_STATUS);
        }
    }

    
    private void doCreate()
    {
        try
        {
            ContainerNode cnode = new ContainerNode(target);
            Node nodeRtn = client.createNode(cnode);
            log.info("created: " + nodeRtn.getUri());
        }
        catch(Throwable t)
        {
            msg("failed to create: " + target);
            if (t.getMessage() != null)
                msg("          reason: " + t.getMessage());
            else
                msg("          reason: " + t);
            System.exit(NET_STATUS);
        }
    }

    private void doView()
    {
        try
        {
            Node n = client.getNode(target.getPath());
            msg(getType(n) + ": " + n.getUri());
            msg("creator: " + safePropertyRef(n, VOS.PROPERTY_URI_CREATOR));
            msg("last modified: " + safePropertyRef(n, VOS.PROPERTY_URI_DATE));
            msg("readable by: " + safePropertyRef(n, VOS.PROPERTY_URI_GROUPREAD));
            msg("writable by: " + safePropertyRef(n, VOS.PROPERTY_URI_GROUPWRITE));
            if (n instanceof ContainerNode)
            {
                ContainerNode cn = (ContainerNode) n;
                msg("child nodes:");
                for (Node child : cn.getNodes())
                {
                    //msg("\tchild: " + child.getName() + "\t" + getType(child) + "\t" + child.getUri());
                    msg("\t" + child.getUri());
                }
            }
            else if (n instanceof DataNode)
            {
                msg("size: " + safePropertyRef(n, VOS.PROPERTY_URI_CONTENTLENGTH));
                msg("type: " + safePropertyRef(n, VOS.PROPERTY_URI_TYPE));
                msg("encoding: " + safePropertyRef(n, VOS.PROPERTY_URI_CONTENTENCODING));
                msg("md5sum: " + safePropertyRef(n, VOS.PROPERTY_URI_CONTENTMD5));
            }
            else
            {
                log.debug("class of returned node: " + n.getClass().getName());
            }
        }
        catch(NodeNotFoundException ex)
        {
            msg("not found: " + target);
            System.exit(NET_STATUS);
        }
    }

    private void copyToVOSpace()
        throws Exception
    {
        Node n = null;
        try { n = client.getNode(destination.getPath()); }
        catch(NodeNotFoundException ignore) { }
        if (n != null && !(n instanceof DataNode))
            throw new IllegalArgumentException("destination is an existing node of type " + getType(n));

        DataNode dnode = null;
        if (n != null)
        {
            log.info("overwriting existing data node: " + destination);
            dnode = (DataNode) n;
            // update props if necessary
            if (copyProperties(dnode))
            {
                log.info("updating node properties: " + destination);
                dnode = (DataNode) client.setNode(dnode);
            }
        }
        else
        {
            log.info("creating new data node: " + destination);
            dnode = new DataNode(new VOSURI(this.destination));
            copyProperties(dnode);
            dnode = (DataNode) client.createNode(dnode);
        }



        View dview = new View(new URI(VOS.VIEW_DEFAULT));

        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(new Protocol(VOS.PROTOCOL_HTTPS_PUT, baseUrl, null));

        Transfer transfer = new Transfer();
        transfer.setTarget(dnode);
        transfer.setView(dview);
        transfer.setProtocols(protocols);
        transfer.setDirection(this.transferDirection);

        ClientTransfer clientTransfer = new ClientTransfer(this.client.pushToVoSpace(transfer));
        log.debug(clientTransfer.toXmlString());

        log.debug("this.source: " + source);
        File fileToUpload = new File(source);
        clientTransfer.doUpload(fileToUpload);
        Node node = clientTransfer.getTarget();
        log.debug("clientTransfer getTarget: " + node);
        Node nodeRtn = this.client.getNode(node.getPath());
        log.debug("Node returned from getNode, after doUpload: " + VOSClientUtil.xmlString(nodeRtn));
    }

    private void copyFromVOSpace()
        throws Exception
    {
        DataNode dnode = new DataNode(new VOSURI(source));
        View dview = new View(new URI(VOS.VIEW_DEFAULT));

        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(new Protocol(VOS.PROTOCOL_HTTPS_GET, baseUrl, null));

        Transfer transfer = new Transfer();
        transfer.setTarget(dnode);
        transfer.setView(dview);
        transfer.setProtocols(protocols);
        transfer.setDirection(transferDirection);

        ClientTransfer clientTransfer = new ClientTransfer(client.pullFromVoSpace(transfer));
        log.debug(clientTransfer.toXmlString());

        log.debug("this.source: " + source);
        File fileToSave = new File(destination);
        if (fileToSave.exists())
            log.info("overwriting existing file: " + destination);
        clientTransfer.doDownload(fileToSave);
        Node node = clientTransfer.getTarget();
        log.debug("clientTransfer getTarget: " + node);
    }

    // copy properties specified on command-line to the specified node
    private boolean copyProperties(Node n)
    {
        List<NodeProperty> cur = n.getProperties();
        if (cur == null)
            n.setProperties(properties);
        else
        {
            Map<String,NodeProperty> map = new HashMap<String,NodeProperty>();
            // copy current
            for (NodeProperty np : cur)
                map.put(np.getPropertyURI(), np);
            // replace with specified values
            for (NodeProperty np : properties)
                map.put(np.getPropertyURI(), np);
            cur.clear();
            cur.addAll(map.values());
        }
        // return true if some props were set
        return (properties.size() > 0);
    }

    private static String ZERO_LENGTH = "";
    private String safePropertyRef(Node n, String key)
    {
        if (n == null || key == null)
            return ZERO_LENGTH;
        NodeProperty p = n.findProperty(key);
        if (p == null)
            return ZERO_LENGTH;
        String ret = p.getPropertyValue();
        if (ret == null)
            return ZERO_LENGTH;
        return ret;
    }
    private String getType(Node n)
    {
        if (n instanceof ContainerNode)
            return "container";
        if (n instanceof DataNode)
            return "data";
        return ZERO_LENGTH;
    }

    /**
     * Initialize command member variables based on arguments passed in.
     * 
     * @param argMap
     */
    private void init(ArgumentMap argMap)
    {
        URI serverUri = null;
        try
        {
            validateInitSSL(argMap);
        }
        catch(Exception ex)
        {
            log.error("failed to initialise SSL from certificates: " + ex.getMessage());
            System.exit(INIT_STATUS);
        }

        try
        {
            if (this.operation.equals(Operation.COPY))
            {
                String strSrc = argMap.getValue(ARG_SRC);
                String strDest = argMap.getValue(ARG_DEST);
                if (!strSrc.startsWith(VOS_PREFIX) && strDest.startsWith(VOS_PREFIX))
                {
                    this.transferDirection = Direction.pushToVoSpace;
                    try
                    {
                        this.destination = new URI(strDest);
                        serverUri = new VOSURI(strDest).getServiceURI();
                    }
                    catch (URISyntaxException e)
                    {
                        throw new IllegalArgumentException("Invalid VOS URI: " + strDest);
                    }
                    File f = new File(strSrc);
                    if (!f.exists() || !f.canRead())
                        throw new IllegalArgumentException("Source file " + strSrc + " does not exist or cannot be read.");
                    try
                    {
                        this.source = new URI("file", f.getAbsolutePath(), null);
                    }
                    catch (URISyntaxException e)
                    {
                        throw new IllegalArgumentException("Invalid file path: " + strSrc);
                    }
                }
                else if (strSrc.startsWith(VOS_PREFIX) && !strDest.startsWith(VOS_PREFIX))
                {
                    this.transferDirection = Direction.pullFromVoSpace;
                    try
                    {
                        serverUri = new VOSURI(strSrc).getServiceURI();
                        this.source = new URI(strSrc);
                    }
                    catch (URISyntaxException e)
                    {
                        throw new IllegalArgumentException("Invalid VOS URI: " + strSrc);
                    }
                    File f = new File(strDest);
                    if (f.exists())
                    {
                        if (!f.canWrite()) throw new IllegalArgumentException("Destination file " + strDest + " is not writable.");
                    }
                    else
                    {
                        File parent = f.getParentFile();
                        if (parent == null)
                        {
                            String cwd = System.getProperty("user.dir");
                            parent = new File(cwd);
                        }
                        if (parent.isDirectory())
                        {
                            if (!parent.canWrite())
                                throw new IllegalArgumentException("The parent directory of destination file " + strDest
                                        + " is not writable.");
                        }
                        else
                            throw new IllegalArgumentException("Destination file " + strDest + " is not within a directory.");
                    }
                    this.destination = f.toURI();
                }
                else
                    throw new UnsupportedOperationException("The type of your copy operation is not supported yet.");
            }
            else
            {
                String strTarget = argMap.getValue(ARG_TARGET);
                try
                {
                    this.target = new VOSURI(strTarget);
                    serverUri = this.target.getServiceURI();
                }
                catch (URISyntaxException e)
                {
                    throw new IllegalArgumentException("Invalid VOS URI: " + strTarget);
                }
            }
        }
        catch(NullPointerException nex)
        {
            log.error("BUG", nex);
            System.exit(-1);
        }
        catch(Exception ex)
        {
            log.error(ex.toString());
            System.exit(INIT_STATUS);
        }

        try
        {
            URL baseURL = registryClient.getServiceURL(serverUri, "https");
            if (baseURL == null)
            {
                log.error("failed to find service URL for " + serverUri);
                System.exit(INIT_STATUS);
            }
            this.baseUrl = baseURL.toString();
        }
        catch (MalformedURLException e)
        {
            log.error("failed to find service URL for " + serverUri);
            log.error("reason: " + e.getMessage());
            System.exit(INIT_STATUS);
        }
        this.client = new VOSpaceClient(baseUrl);
        log.info("server uri: " + serverUri);
        log.info("base url: " + this.baseUrl);
    }

    private void validateInitSSL(ArgumentMap argMap)
    {
        String strCert = argMap.getValue(ARG_CERT);
        String strKey = argMap.getValue(ARG_KEY);

        this.certFile = new File(strCert);
        this.keyFile = new File(strKey);

        StringBuffer sbSslMsg = new StringBuffer();
        boolean sslError = false;
        if (!certFile.exists())
        {
            sbSslMsg.append("Certificate file " + strCert + " does not exist. \n");
            sslError = true;
        }
        if (!keyFile.exists())
        {
            sbSslMsg.append("Key file " + strKey + " does not exist. \n");
            sslError = true;
        }
        if (!sslError)
        {
            if (!certFile.canRead())
            {
                sbSslMsg.append("Certificate file " + strCert + " cannot be read. \n");
                sslError = true;
            }
            if (!keyFile.canRead())
            {
                sbSslMsg.append("Key file " + strKey + " cannot be read. \n");
                sslError = true;
            }
        }
        if (sslError)
        {
            throw new IllegalArgumentException(sbSslMsg.toString());
        }
        SSLUtil.initSSL(this.certFile, this.keyFile);
    }

    /**
     * @param argMap
     */
    private void validateCommand(ArgumentMap argMap) throws IllegalArgumentException
    {
        int numOp = 0;
        if (argMap.isSet(ARG_VIEW))
        {
            numOp++;
            this.operation = Operation.VIEW;
        }
        if (argMap.isSet(ARG_CREATE))
        {
            numOp++;
            this.operation = Operation.CREATE;
        }
        if (argMap.isSet(ARG_DELETE))
        {
            numOp++;
            this.operation = Operation.DELETE;
        }
        if (argMap.isSet(ARG_SET))
        {
            numOp++;
            this.operation = Operation.SET;
        }
        if (argMap.isSet(ARG_COPY))
        {
            numOp++;
            this.operation = Operation.COPY;
        }

        if (numOp == 0)
            throw new IllegalArgumentException("One operation should be defined.");
        else if (numOp > 1) throw new IllegalArgumentException("Only one operation can be defined.");

        return;
    }

    /**
     * @param argMap
     */
    private void validateCommandArguments(ArgumentMap argMap)
        throws IllegalArgumentException
    {
        String strCert = argMap.getValue(ARG_CERT);
        String strKey = argMap.getValue(ARG_KEY);
        if (strCert == null || strKey == null) throw new IllegalArgumentException("Argument cert and key are all required.");

        if (this.operation.equals(Operation.COPY))
        {
            String strSrc = argMap.getValue(ARG_SRC);
            if (strSrc == null) throw new IllegalArgumentException("Argument src is required for " + this.operation);

            String strDest = argMap.getValue(ARG_DEST);
            if (strDest == null) throw new IllegalArgumentException("Argument dest is required for " + this.operation);
        }
        else
        {
            String strTarget = argMap.getValue(ARG_TARGET);
            if (strTarget == null) throw new IllegalArgumentException("Argument target is required for " + this.operation);
        }

        // optional properties
        this.properties = new ArrayList<NodeProperty>();
        
        String propFile = argMap.getValue(ARG_PROP);
        if (propFile != null)
        {
            File f = new File(propFile);
            if (f.exists())
            {
                if (f.canRead())
                {
                    try
                    {
                        Properties p = new Properties();
                        p.load(new FileReader(f));
                        for ( String key : p.stringPropertyNames())
                        {
                            String val = p.getProperty(key);
                            properties.add(new NodeProperty(key, val));
                        }
                    }
                    catch(IOException ex)
                    {
                        log.info("failed to read properties file: "
                                + f.getAbsolutePath()
                                + "(" + ex.getMessage() + ", skipping)");
                    }
                }
                else
                    log.info("cannot read properties file: "
                            + f.getAbsolutePath() + " (permission denied, skipping)");
            }
            else
                log.info("cannot read properties file: "
                        + f.getAbsolutePath() + " (does not exist, skipping)");
        }

        String contentType = argMap.getValue(ARG_CONTENT_TYPE);
        String contentEncoding = argMap.getValue(ARG_CONTENT_ENCODING);
        String contentMD5 = argMap.getValue(ARG_CONTENT_MD5);
        String groupRead = argMap.getValue(ARG_GROUP_READ);
        String groupWrite = argMap.getValue(ARG_GROUP_WRITE);

        if (contentType != null)
            properties.add(new NodeProperty(VOS.PROPERTY_URI_TYPE, contentType));
        if (contentEncoding != null)
            properties.add(new NodeProperty(VOS.PROPERTY_URI_CONTENTENCODING, contentEncoding));
        if (contentMD5 != null)
            properties.add(new NodeProperty(VOS.PROPERTY_URI_CONTENTMD5, contentMD5));
        if (groupRead != null)
            properties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, groupRead));
        if (groupWrite != null)
            properties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, groupWrite));

    }

    /**
         * @return The usage string
         */
    public static void usage()
    {
        String[] um = {
        /*
         * Note: When using "Format" in Eclipse, shorter lines in this string array are squeezed into one line.
         * This makes it hard to read or edit.
         * 
         * A workaround is, lines are purposely extended with blank spaces to a certain length,
         * where the EOL is at about column 120 (including leading indenting spaces).
         * 
         * In this way, it's still easy to read and edit and the formatting operation does not change it's layout.
         * 
         */
        "Usage: java -jar VOSpaceClient.jar [-v|--verbose|-d|--debug]  ...                                 ",
                "                                                                                                  ",
                "Help:                                                                                             ",
                "java -jar VOSpaceClient.jar <-h | --help>                                                         ",
                "                                                                                                  ",
                "Create node:                                                                                      ",
                "java -jar VOSpaceClient.jar  [-v|--verbose|-d|--debug]                                            ",
                "   --cert=<SSL certificate file> --key=<SSL key file>                                             ",
                "   --create --target=<target URI>                                                                  ",
                "   [--group-read=<group URI>]                                                                      ",
                "   [--group-write=<group URI>]                                                                     ",
                "   [--prop=<properties file>]                                                                      ",
                "                                                                                                  ",
                "Note: --create defaults to creating a ContainerNode (directory). Creating                         ",
                "other types of nodes specifically is not suppoerted at this time.                                 ",
                "                                                                                                  ",
                "Copy file:                                                                                        ",
                "java -jar VOSpaceClient.jar  [-v|--verbose|-d|--debug]                                            ",
                "   --cert=<SSL certificate file> --key=<SSL key file>                                             ",
                "   --copy --src=<source URI> --dest=<destination URI>                                            ",
                "   [--content-type=<mimetype of source>]                                                           ",
                "   [--content-encoding=<encoding of source>]                                                       ",
                "   [--group-read=<group URI>]                                                                      ",
                "   [--group-write=<group URI>]                                                                     ",
                "   [--prop=<properties file>]                                                                      ",
                "                                                                                                  ",
                "Note: One of --src and --target may be a \"vos\" URI and the other may be an                        ",
                "absolute or relative path to a file. If the target node does not exist, a                         ",
                "DataNode is created and data copied. If it does exist, the data (and                              ",
                "properties?) are overwritten.                                                                     ",
                "                                                                                                  ",
                "View node:                                                                                        ",
                "java -jar VOSpaceClient.jar  [-v|--verbose|-d|--debug]                                            ",
                "   --cert=<SSL certificate file> --key=<SSL key file>                                             ",
                "   --view --target=<target URI>                                                                    ",
                "   [--group-read=<group URI>]                                                                      ",
                "   [--group-write=<group URI>]                                                                     ",
                "                                                                                                  ",
                "Delete node:                                                                                      ",
                "java -jar VOSpaceClient.jar  [-v|--verbose|-d|--debug]                                            ",
                "   --cert=<SSL certificate file> --key=<SSL key file>                                             ",
                "   --delete --target=<target URI>                                                                  ",
                "   [--content-type=<mimetype of source>]                                                           ",
                "   [--content-encoding=<encoding of source>]                                                       ",
                "   [--group-read=<group URI>]                                                                      ",
                "   [--group-write=<group URI>]                                                                     ",
                "                                                                                                  ",
                "Set node:                                                                                         ",
                "java -jar VOSpaceClient.jar  [-v|--verbose|-d|--debug]                                            ",
                "   --cert=<SSL certificate file> --key=<SSL key file>                                             ",
                "   --set --target=<target URI>                                                                     ",
                "   [--content-type=<mimetype of source>]                                                           ",
                "   [--content-encoding=<encoding of source>]                                                       ",
                "   [--group-read=<group URI>]                                                                      ",
                "   [--group-write=<group URI>]                                                                     ",
                "   [--prop=<properties file>]                                                                      ", "" };
        for (String line : um)
            msg(line);
    }
}
