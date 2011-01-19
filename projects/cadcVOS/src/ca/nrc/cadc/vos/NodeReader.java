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

package ca.nrc.cadc.vos;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import ca.nrc.cadc.uws.util.StringUtil;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import ca.nrc.cadc.vos.VOS.NodeBusyState;
import ca.nrc.cadc.xml.XmlUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructs a Node from an XML source.
 *
 * @author jburke
 */
public class NodeReader
{
    private static final String VOSPACE_SCHEMA_URL =
            "http://www.ivoa.net/xml/VOSpace/v2.0";
    private static final String VOSPACE_SCHEMA_RESOURCE = "VOSpace-2.0.xsd";
    private static final String UWS_SCHEMA_URL =
            "http://www.ivoa.net/xml/UWS/v1.0";
    private static final String UWS_SCHEMA_RESOURCE = "UWS-v1.0.xsd";
    private static final String XLINK_SCHEMA_URL =
            "http://www.w3.org/1999/xlink";
    private static final String XLINK_SCHEMA_RESOURCE = "XLINK.xsd";
    
    private static final Logger LOGGER = Logger.getLogger(NodeReader.class);
    
    private static final String vospaceSchemaUrl;
    private static final String uwsSchemaUrl;
    private static final String xlinkSchemaUrl;
    static
    {
        vospaceSchemaUrl = XmlUtil.getResourceUrlString(VOSPACE_SCHEMA_RESOURCE,
                                                        NodeReader.class);
        LOGGER.debug("vospaceSchemaUrl: " + vospaceSchemaUrl);
        
        uwsSchemaUrl = XmlUtil.getResourceUrlString(UWS_SCHEMA_RESOURCE,
                                                    NodeReader.class);
        LOGGER.debug("uwsSchemaUrl: " + uwsSchemaUrl);
        
        xlinkSchemaUrl = XmlUtil.getResourceUrlString(XLINK_SCHEMA_RESOURCE,
                                                      NodeReader.class);
        LOGGER.debug("xlinkSchemaUrl: " + xlinkSchemaUrl);
    }

    
    protected Map<String, String> schemaMap;
    protected Namespace xsiNamespace;

    public NodeReader()
    {
        schemaMap = new HashMap<String, String>();
        schemaMap.put(VOSPACE_SCHEMA_URL, vospaceSchemaUrl);
        schemaMap.put(UWS_SCHEMA_URL, uwsSchemaUrl);
        schemaMap.put(XLINK_SCHEMA_URL, xlinkSchemaUrl);

        xsiNamespace = Namespace.getNamespace(
                "http://www.w3.org/2001/XMLSchema-instance");
    }

    /**
     *  Construct a Node from an XML String source.
     *
     * @param xml String of the XML.
     * @return Node Node.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    public Node read(final String xml) throws NodeParsingException
    {
        if (xml == null)
        {
            throw new IllegalArgumentException("XML must not be null");
        }

        LOGGER.debug("Reading:\n" + xml);

        return read(new StringReader(xml));
    }

    /**
     * Construct a Node from a InputStream.
     *
     * @param in InputStream.
     * @return Node Node.
     *
     * @throws IOException          If the input stream is null or closed.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    public Node read(final InputStream in)
        throws IOException, NodeParsingException
    {
        if (in == null)
        {
            throw new IOException("stream closed");
        }

        InputStreamReader reader;

        try
        {
            reader = new InputStreamReader(in, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported");
        }
        return read(reader);
    }

    /**
     *  Construct a Node from a Reader.
     *
     * @param reader Reader.
     * @return Node Node.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    public Node read(final Reader reader) throws NodeParsingException
    {
        if (reader == null)
        {
            throw new IllegalArgumentException("reader must not be null");
        }

        // Create a JDOM Document from the XML
        Document document;
        try
        {
            document = XmlUtil.validateXml(reader, schemaMap);
        }
        catch (JDOMException jde)
        {
            String error = "XML failed schema validation: " + jde.getMessage();
            LOGGER.error(error, jde);
            throw new NodeParsingException(error, jde);
        }
        catch (IOException ioe)
        {
            String error = "Error reading XML: " + ioe.getMessage();
            LOGGER.error(error, ioe);
            throw new NodeParsingException(error, ioe);
        }

        // Root element and namespace of the Document
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        LOGGER.debug("node namespace uri: " + namespace.getURI());
        LOGGER.debug("node namespace prefix: " + namespace.getPrefix());

        /* Node base elements */
        // uri attribute of the node element
        String uri = root.getAttributeValue("uri");
        if (uri == null)
        {
            String error = "uri attribute not found in root element";
            LOGGER.error(error);
            throw new NodeParsingException(error);
        }
        LOGGER.debug("node uri: " + uri);

        // Get the xsi:type attribute which defines the Node class
        String xsiType = root.getAttributeValue("type", xsiNamespace);
        if (xsiType == null)
        {
            final String error =
                    "xsi:type attribute not found in node element " + uri;

            LOGGER.error(error);
            throw new NodeParsingException(error);
        }

        // Split the type attribute into namespace and Node type
        String[] types = xsiType.split(":");
        String type = types[1];
        LOGGER.debug("node type: " + type);

        if (type.equals(ContainerNode.class.getSimpleName()))
            return buildContainerNode(root, namespace, uri);
        else if (type.equals(DataNode.class.getSimpleName()))
            return buildDataNode(root, namespace, uri);
        else
            throw new NodeParsingException("unsupported node type " + type);
    }

    /**
     *  Get an String representation of the URL
     *  to the VOSpace schema document.
     *
     * @return String of the VOSpace schema document URL
     */
//    protected String getVOSpaceSchema()
//    {
//        return vospaceSchemaUrl;
//    }

    /**
     * Constructs a ContainerNode from the given root Element of the
     * Document, Document Namespace, and Node path.
     *
     * @param root Root Element of the Document.
     * @param namespace Document Namespace.
     * @param uri Node uri attribute.
     * @return ContainerNode
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    @SuppressWarnings("unchecked")
    protected Node buildContainerNode(final Element root,
                                      final Namespace namespace,
                                      final String uri)
        throws NodeParsingException
    {
        // Instantiate a ContainerNode class
        ContainerNode node;
        try
        {
            node = new ContainerNode(new VOSURI(uri));
        }
        catch (URISyntaxException e)
        {
            String error = "invalid node uri " + uri;
            LOGGER.error(error, e);
            throw new NodeParsingException(error, e);
        }

        // properties element
        node.setProperties(getProperties(root, namespace));

        // nodes element
        Element nodes = root.getChild("nodes", namespace);
        if (nodes == null)
        {
            String error = "nodes element not found in node";
            LOGGER.error(error);
            throw new NodeParsingException(error);
        }

        // list of child nodes
        final List<Element> nodesList = nodes.getChildren("node", namespace);

        for (final Element childNode : nodesList)
        {
            final String childNodeUri = childNode.getAttributeValue("uri");
            final String childNodeType =
                    childNode.getAttributeValue("type", xsiNamespace);

            if (!StringUtil.hasLength(childNodeUri))
            {
                final String error =
                        "uri attribute not found in nodes node element";

                LOGGER.error(error);
                throw new NodeParsingException(error);
            }
            else if (!StringUtil.hasLength(childNodeType))
            {
                final String error =
                        "type attribute not found in node element";

                LOGGER.error(error);
                throw new NodeParsingException(error);
            }

            try
            {
                final String[] types = childNodeType.split(":");
                final String type = types[1];
                final Node n;

                if (type.equals(ContainerNode.class.getSimpleName()))
                {
                    n = new ContainerNode(new VOSURI(childNodeUri));

                    final Element grandChildNodes =
                            childNode.getChild("nodes", namespace);

                    if (grandChildNodes != null)
                    {
                        final List<Element> grandChildNodeList =
                                grandChildNodes.getChildren("node", namespace);

                        for (final Element grandChildNode : grandChildNodeList)
                        {
                            final String grandChildNodeType =
                                    childNode.getAttributeValue("type",
                                                                xsiNamespace);
                            final String grandchildURI =
                                    grandChildNode.getAttributeValue("uri");
                            final Node g;
                            final String[] gtypes =
                                    grandChildNodeType.split(":");
                            final String gtype = gtypes[1];

                            if (gtype.equals(
                                    ContainerNode.class.getSimpleName()))
                            {
                                g = new ContainerNode(
                                        new VOSURI(grandchildURI));
                            }
                            else if (gtype.equals(
                                    DataNode.class.getSimpleName()))
                            {
                                g = new DataNode(new VOSURI(grandchildURI));
                            }
                            else
                            {
                                continue;
                            }

                            ((ContainerNode) n).getNodes().add(g);
                        }
                    }
                }
                else if (type.equals(DataNode.class.getSimpleName()))
                {
                    n = new DataNode(new VOSURI(childNodeUri));
                }
                else
                {
                    LOGGER.warn("Unsupported Node Type: " + type + " (from "
                                + childNodeType + ")");
                    n = null;
                }

                if (n != null)
                {
                    if (childNode.getChild("properties", namespace) != null)
                    {
                        n.setProperties(getProperties(childNode, namespace));
                    }

                    node.getNodes().add(n);
                }
            }
            catch (URISyntaxException e)
            {
                String error = "invalid child node uri " + childNodeUri;
                LOGGER.error(error, e);
                throw new NodeParsingException(error, e);
            }
            LOGGER.debug("added child node: " + childNodeUri);
        }

        return node;
    }

    /**
     * Constructs a DataNode from the given root Element of the
     * Document, Document Namespace, and Node path.
     *
     * @param root Root Element of the Document.
     * @param namespace Document Namespace.
     * @param uri Node uri attribute.
     * @return DataNode.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    protected Node buildDataNode(Element root, Namespace namespace, String uri)
        throws NodeParsingException
    {
        // Instantiate a DataNode class
        DataNode node;
        try
        {
            node = new DataNode(new VOSURI(uri));
        }
        catch (URISyntaxException e)
        {
            String error = "invalid node uri " + uri;
            LOGGER.error(error, e);
            throw new NodeParsingException(error, e);
        }

        // busy attribute
        String busy = root.getAttributeValue("busy");
        if (busy == null)
        {
            String error = "busy element not found in DataNode";
            LOGGER.error(error);
            throw new NodeParsingException(error);
        }

        final boolean isBusy = busy.equalsIgnoreCase("true");
        
        // TODO: BM: Change the XML schema to support the three possible
        // values for the busy state: not busy, busy with read, busy
        // with write.  For now, we'll consider busy to be the more
        // restrictive busy with write.
        if (isBusy)
        {
            node.setBusy(NodeBusyState.busyWithWrite);
        }
        else
        {
            node.setBusy(NodeBusyState.notBusy);
        }
        LOGGER.debug("busy: " + isBusy);

        // properties element
        node.setProperties(getProperties(root, namespace));

        // accepts element
//        TODO: add accepts element
//        node.accepts().addAll(getViews(root, namespace, "accepts"));

        // provides element
//        TODO: add provides element
//        node.provides().addAll(getViews(root, namespace, "provides"));

        return node;
    }

    /**
     * Builds a List of NodeProperty objects from the Document property Elements.
     *
     * @param root Root Element of the Document.
     * @param namespace Document Namespace.
     * @return List of NodeProperty objects.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    @SuppressWarnings("unchecked")
    protected List<NodeProperty> getProperties(Element root,
                                               Namespace namespace)
        throws NodeParsingException
    {
        // properties element
        Element properties = root.getChild("properties", namespace);
        if (properties == null)
        {
            final String error = "properties element not found";

            LOGGER.error(error);
            throw new NodeParsingException(error);
        }

        // new NodeProperty List
        List<NodeProperty> set = new ArrayList<NodeProperty>();

        // properties property elements
        List<Element> propertyList = properties.getChildren("property",
                                                            namespace);
        for (Element property : propertyList)
        {
            String propertyUri = property.getAttributeValue("uri");
            if (propertyUri == null)
            {
                String error = "uri attribute not found in property element "
                               + property;
                LOGGER.error(error);
                throw new NodeParsingException(error);
            }

            // xsi:nil set to true indicates Property is to be deleted
            String xsiNil = property.getAttributeValue("xsi:nil");
            boolean markedForDeletion = false;

            if (xsiNil != null)
            {
                markedForDeletion = xsiNil.equalsIgnoreCase("true");
            }

            // if marked for deletetion, property can not contain text content
            String text = property.getText();
            if (markedForDeletion)
                text = "";

            // create new NodeProperty
            NodeProperty nodeProperty = new NodeProperty(propertyUri, text);

            // set readOnly attribute
            String readOnly = property.getAttributeValue("readOnly");
            if (readOnly != null)
                nodeProperty.setReadOnly(readOnly.equalsIgnoreCase("true"));

            // markedForDeletion attribute
            nodeProperty.setMarkedForDeletion(markedForDeletion);
            set.add(nodeProperty);
        }

        return set;
    }

    /**
     * Builds a List of View objects from the view elements contained within
     * the given parent element.
     *
     * @param root Root Element of the Document.
     * @param namespace Document Namespace.
     * @param parent View Parent Node.
     * @return List of View objects.
     * @throws NodeParsingException if there is an error parsing the XML.
     */
    @SuppressWarnings("unchecked")
    protected List<View> getViews(Element root, Namespace namespace,
                                  String parent)
            throws NodeParsingException
    {
        // view parent element
        Element parentElement = root.getChild(parent, namespace);
        if (parentElement == null)
        {
            String error = parent + " element not found in node";
            LOGGER.error(error);
            throw new NodeParsingException(error);
        }

        // new View List
        List<View> list = new ArrayList<View>();

        // view elements
        final List<Element> viewList = parentElement.getChildren("view",
                                                                 namespace);
        for (final Element view : viewList)
        {
            // view uri attribute
            final String viewUri = view.getAttributeValue("uri");
            if (viewUri == null)
            {
                String error = "uri attribute not found in " + parent
                               + " view element";
                LOGGER.error(error);
                throw new NodeParsingException(error);
            }

            LOGGER.debug(parent + "view uri: " + viewUri);

            // view original attribute
            final String original = view.getAttributeValue("original");

            if (original != null)
            {
                final boolean isOriginal = original.equalsIgnoreCase("true");
                LOGGER.debug(parent + " view original: " + isOriginal);
            }

            final List<Element> paramList = view.getChildren("param",
                                                             namespace);

            for (final Element param : paramList)
            {
                final String paramUri = param.getAttributeValue("uri");

                if (paramUri == null)
                {
                    final String error = "param uri attribute not found in "
                                         + "accepts view element";
                    LOGGER.error(error);
                    throw new NodeParsingException(error);
                }

                LOGGER.debug("accepts view param uri: " + paramUri);
                // TODO: what are params???
            }
        }

        return list;
    }

}
