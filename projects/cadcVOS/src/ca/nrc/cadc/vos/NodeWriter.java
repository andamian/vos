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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ca.nrc.cadc.util.StringBuilderWriter;
import ca.nrc.cadc.vos.VOS.NodeBusyState;

/**
 * Writes a Node as XML to an output.
 * 
 * @author jburke
 */
public class NodeWriter
{
    // Search Results formatting.
    private Search.Results results;


    /*
     * The VOSpace Namespaces.
     */
    protected static Namespace defaultNamespace;
    protected static Namespace vosNamespace;
    protected static Namespace xsiNamespace;
    
    static
    {
        defaultNamespace = Namespace.getNamespace("http://www.ivoa.net/xml/VOSpace/v2.0");
        vosNamespace = Namespace.getNamespace("vos", "http://www.ivoa.net/xml/VOSpace/v2.0");
        xsiNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    private String stylesheetURL = null;

    public NodeWriter()
    {
        this(new Search.Results(Search.Results.Detail.MAX));
    }

    public NodeWriter(Search.Results results)
    {
        this.results = results;
    }

    public void setStylesheetURL(String stylesheetURL)
    {
        this.stylesheetURL = stylesheetURL;
    }
    
    public String getStylesheetURL()
    {
        return stylesheetURL;
    }

    /**
     * Write a ContainerNode to a StringBuilder.
     *
     * @param node              The Node to write.
     * @param builder           The StringBuilder target.
     * @throws IOException      If anything goes wrong.
     */
    public void write(ContainerNode node, StringBuilder builder)
            throws IOException
    {
        write(node, new StringBuilderWriter(builder));
    }

    /**
     * Write a ContainerNode to an OutputStream.
     *
     * @param node Node to write.
     * @param out OutputStream to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(ContainerNode node, OutputStream out) throws IOException
    {
        OutputStreamWriter outWriter;
        try
        {
            outWriter = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        write(node, new BufferedWriter(outWriter));
    }

    /**
     * A wrapper to write node without specifying its type
     * 
     * @param node              The Node to write.
     * @param writer            The Writer to write to.
     * @throws IOException      If anything goes wrong.
     */
    public void write(Node node, Writer writer) throws IOException
    {
        if (node instanceof ContainerNode)
        {
            write((ContainerNode) node, writer);
        }
        else if (node instanceof DataNode)
        {
            write((DataNode) node, writer);
        }
    }

    /**
     * Write a DataNode to a StringBuilder.
     * 
     * @param node Node to write.
     * @param builder StringBuilder to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, StringBuilder builder) throws IOException
    {
        write(node, new StringBuilderWriter(builder));
    }

    /**
     * Write a DataNode to an OutputStream.
     *
     * @param node Node to write.
     * @param out OutputStream to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, OutputStream out) throws IOException
    {
        OutputStreamWriter outWriter;
        try
        {
            outWriter = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        write(node, new BufferedWriter(outWriter));
    }
    
    /**
     * Write a ContainerNode to a Writer.
     *
     * @param node Node to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(ContainerNode node, Writer writer) throws IOException
    {
        // Create the root node element
        Element root = getRootElement(node);

        // properties element
//        root.addContent(getPropertiesElement(node));
        
        // accepts element
//        root.addContent(getAcceptsElement(node));
        
        // provides element
//        root.addContent(getProvidesElement(node));
        
        // nodes element
//        root.addContent(getNodesElement(node));

        // write out the Document
        write(root, writer);
    }

    /**
     * Write a DataNode to a Writer.
     *
     * @param node Node to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void write(DataNode node, Writer writer) throws IOException
    {
        // Create the root node element
        Element root = getRootElement(node);

        // busy attribute
        root.setAttribute("busy", (node.getBusy().equals(NodeBusyState.notBusy)
                                   ? "false" : "true"));

        // properties element
//        root.addContent(getPropertiesElement(node));
        
        // accepts element
//        root.addContent(getAcceptsElement(node));
        
        // provides element
//        root.addContent(getProvidesElement(node));

        // write out the Document
        write(root, writer);
    }

    /**
     *  Build the root Element of a Node.
     *
     * @param node Node.
     * @return root Element.
     */
    protected Element getRootElement(final Node node)
    {
        // Create the root element (node).
        final Element root = new Element("node", defaultNamespace);
        root.addNamespaceDeclaration(vosNamespace);
        root.addNamespaceDeclaration(xsiNamespace);

        final NodeElementFormatter nodeElementFormatter =
                new NodeElementFormatter(root, node, false);
        nodeElementFormatter.format();

        return nodeElementFormatter.getNodeElement();
    }

    /**
     * Build the properties Element of a Node.
     *
     * @param node Node.             The node to get properties for.
     * @param propertyURIFilter      URIs to filter on (inclusive).
     * @return properties Element.
     */
    protected Element getPropertiesElement(final Node node,
                                           final String... propertyURIFilter)
    {
        final Element properties = new Element("properties", defaultNamespace);

        for (NodeProperty nodeProperty : node.getProperties())
        {
            final String propertyURI = nodeProperty.getPropertyURI();

            if ((propertyURIFilter != null) && (propertyURIFilter.length > 0))
            {
                boolean found = false;

                for (final String filteredURI: propertyURIFilter)
                {
                    if (propertyURI.equals(filteredURI))
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    continue;
                }
            }

            Element property = new Element("property", defaultNamespace);
            property.setAttribute("uri", propertyURI);
            property.setText(nodeProperty.getPropertyValue());
            property.setAttribute("readOnly", (nodeProperty.isReadOnly()
                                               ? "true" : "false"));
            properties.addContent(property);
        }
        return properties;
    }
    
    /**
     * Build the accepts Element of a Node.
     *
     * @param node Node.
     * @return accepts Element.
     */
    protected Element getAcceptsElement(Node node)
    {
        Element accepts = new Element("accepts", defaultNamespace);
        for (URI viewURI : node.accepts())
        {
            Element viewElement = new Element("view", defaultNamespace);
            viewElement.setAttribute("uri", viewURI.toString());
            accepts.addContent(viewElement);
        }
        return accepts;
    }
    
    /**
     * Build the accepts Element of a Node.
     *
     * @param node Node.
     * @return accepts Element.
     */
    protected Element getProvidesElement(Node node)
    {
        Element provides = new Element("provides", defaultNamespace);
        for (URI viewURI : node.provides())
        {
            Element viewElement = new Element("view", defaultNamespace);
            viewElement.setAttribute("uri", viewURI.toString());
            provides.addContent(viewElement);
        }
        return provides;
    }

    /**
     * Build the capabilities Element of a Node.
     *
     * This option is not supported, but is necessary to appear in some cases.
     *
     * @param node      The node to build from.
     * @return          The resulting Element.
     */
    protected Element getCapabilitiesElement(final Node node)
    {
        return new Element("capabilities", defaultNamespace);
    }


    /**
     * Build the nodes Element of a ContainerNode.
     * 
     * @param node Node.
     * @return nodes Element.
     */
    protected Element getNodesElement(ContainerNode node)
    {
        final Element nodes = new Element("nodes", defaultNamespace);
        final List<Node> currentChildNodes = node.getNodes();
        final Collection<Node> childNodes;

        if ((getResults() != null)
            && (getResults().getLimit() != null)
            && (getResults().getLimit() < currentChildNodes.size()))
        {
            childNodes = currentChildNodes.subList(0, getResults().getLimit());
        }
        else
        {
            childNodes = currentChildNodes;
        }

        for (Node childNode : childNodes)
        {
            final Element nodeElement = new Element("node", defaultNamespace);
            final NodeElementFormatter nodeElementFormatter =
                    new NodeElementFormatter(nodeElement, childNode, true);

            nodeElementFormatter.format();

            final Element childNodeElement =
                    nodeElementFormatter.getNodeElement();

            // Container nodes demand to have their child elements set.
            if (childNode instanceof ContainerNode)
            {
                // Only add an empty properties tag if there isn't already one.
                if (childNodeElement.getChild("properties",
                                              defaultNamespace) == null)
                {
                    childNodeElement.addContent(new Element("properties",
                                                            defaultNamespace));
                }

                if (childNodeElement.getChild("accepts",
                                              defaultNamespace) == null)
                {
                    childNodeElement.addContent(getAcceptsElement(childNode));
                }

                if (childNodeElement.getChild("provides",
                                              defaultNamespace) == null)
                {
                    childNodeElement.addContent(getProvidesElement(childNode));
                }

                if (childNodeElement.getChild("capabilities",
                                              defaultNamespace) == null)
                {
                    childNodeElement.addContent(
                            getCapabilitiesElement(childNode));
                }

                if (childNodeElement.getChild("nodes",
                                              defaultNamespace) == null)
                {
                    childNodeElement.addContent(new Element("nodes",
                                                            defaultNamespace));
                }
            }

            nodes.addContent(childNodeElement);
        }

        return nodes;
    }

    /**
     * Write to root Element to a writer.
     *
     * @param root Root Element to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    @SuppressWarnings("unchecked")
    protected void write(Element root, Writer writer) throws IOException
    {
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        Document document = new Document(root);
        if (stylesheetURL != null)
        {
            Map<String, String> instructionMap = new HashMap<String, String>(2);
            instructionMap.put("type", "text/xsl");
            instructionMap.put("href", stylesheetURL);
            ProcessingInstruction pi =
                    new ProcessingInstruction("xml-stylesheet",
                                              instructionMap);
            document.getContent().add(0, pi);
        }
        outputter.output(document, writer);
    }

    public Search.Results getResults()
    {
        return results;
    }

    public void setResults(final Search.Results results)
    {
        this.results = results;
    }


    /**
     * Format XML Node Elements based on some given Search Results criterion.
     */
    public class NodeElementFormatter
    {
        private Element nodeElement;
        private Node node;
        private boolean childNode;


        /**
         * The NodeElement to format.
         *
         * @param nodeElement   The Element to format.
         * @param node          The node whose information to format.
         * @param childNode     Whether this Node should be treated as a child
         *                      node.
         */
        public NodeElementFormatter(final Element nodeElement,
                                    final Node node, final boolean childNode)
        {
            this.nodeElement = nodeElement;
            this.node = node;
            this.childNode = childNode;
        }


        /**
         * Given this NodeWriter's Search criteria, format the given detail
         * for the given node element, which can then be retrieved.
         */
        public void format()
        {
            final Node n = getNode();
            final Element e = getNodeElement();

            e.setAttribute("uri", getNodeURI());
            e.setAttribute("type", "vos:" + getNodeTypeName(), xsiNamespace);

            if ((getResults() != null) && (getResults().getDetail() != null))
            {
                final Search.Results.Detail detail = getResults().getDetail();

                switch (detail)
                {
                    case PROPERTIES:
                    {
                        if (!isChildNode())
                        {
                            e.addContent(getPropertiesElement(n));
                        }

                        break;
                    }

                    case MAX:
                    {
                        final Element propertiesElement =
                                getPropertiesElement(n,
                                                     VOS.PROPERTY_URI_CONTENTLENGTH,
                                                     VOS.PROPERTY_URI_DATE);
                        e.addContent(propertiesElement);

                        e.addContent(getAcceptsElement(n));
                        e.addContent(getProvidesElement(n));
                        e.addContent(getCapabilitiesElement(n));

                        // Continue on to the MIN section.
                    }

                    case MIN:
                    default:
                    {
                        if (n instanceof ContainerNode)
                        {
                            final ContainerNode container = (ContainerNode) n;

                            if (!isChildNode())
                            {
                                e.addContent(getNodesElement(container));
                            }
                            else
                            {
                                final Element grandChildNodes =
                                        new Element("nodes", defaultNamespace);
                                // Show the grandchildren minimally.
                                for (final Node childNode
                                        : container.getNodes())
                                {
                                    final Element childNodeElement =
                                            new Element("node",
                                                        defaultNamespace);
                                    childNodeElement.setAttribute("uri",
                                                                  childNode.getUri().toString());
                                    grandChildNodes.addContent(childNodeElement);
                                }

                                e.addContent(grandChildNodes);
                            }
                        }
                    }
                }
            }
        }

        public String getNodeURI()
        {
            return getNode().getUri().toString();
        }

        public String getNodeTypeName()
        {
            return getNode().getClass().getSimpleName();
        }

        public Element getNodeElement()
        {
            return nodeElement;
        }

        public Node getNode()
        {
            return node;
        }

        public boolean isChildNode()
        {
            return childNode;
        }
    }
}
