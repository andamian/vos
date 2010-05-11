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

package ca.nrc.cadc.vos.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.DataNode;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeProperty;

/**
 * Class to map a result set into a Node object.
 */
public class NodeMapper implements RowMapper
{
    
    public static String getDatabaseTypeRepresentation(Node node)
    {
        if (node instanceof DataNode)
        {
            return "D";
        }
        if (node instanceof ContainerNode)
        {
            return "C";
        }
        throw new IllegalStateException("Unknown node type: " + node);
    }

    /**
     * Map the row to the appropriate type of node object.
     */
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException
    {

        long nodeID = rs.getLong("nodeID");
        String name = rs.getString("name");
        long parentID = rs.getLong("parentID");
        
        String groupRead = rs.getString("groupRead");
        String groupWrite = rs.getString("groupWrite");
        String owner = rs.getString("owner");
        
        long contentLength = rs.getLong("contentLength");
        String contentType = rs.getString("contentType");
        String contentEncoding = rs.getString("contentEncoding");
        byte[] contentMD5 = rs.getBytes("contentMD5");
        
        ContainerNode parent = null;
        if (parentID != 0)
        {
            parent = new ContainerNode();
            parent.appData = new NodeID(parentID);
        }

        String typeString = rs.getString("type");
        char type = typeString.charAt(0);
        Node node = null;

        if (ContainerNode.DB_TYPE == type)
        {
            node = new ContainerNode();
        }
        else if (DataNode.DB_TYPE == type)
        {
            node = new DataNode();
        }
        else
        {
            throw new IllegalStateException("Unknown node database type: "
                    + type);
        }
        
        node.appData = new NodeID(nodeID);

        node.setName(name);
        node.setParent(parent);
        
        node.setGroupRead(groupRead);
        node.setGroupWrite(groupWrite);
        node.setOwner(owner);
        
        if (contentLength != 0)
        {
            node.getProperties().add(new NodeProperty(NodePropertyMapper.PROPERTY_CONTENTLENGTH_URI, new Long(contentLength).toString()));
        }
        if (contentType != null && contentType.trim().length() > 0)
        {
            node.getProperties().add(new NodeProperty(NodePropertyMapper.PROPERTY_CONTENTTYPE_URI, contentType));
        }
        if (contentEncoding != null && contentEncoding.trim().length() > 0)
        {
            node.getProperties().add(new NodeProperty(NodePropertyMapper.PROPERTY_CONTENTENCODING_URI, contentEncoding));
        }
        if (contentMD5 != null)
        {
            node.getProperties().add(new NodeProperty(NodePropertyMapper.PROPERTY_CONTENTMD5_URI, contentMD5.toString()));
        }

        return node;
    }

}