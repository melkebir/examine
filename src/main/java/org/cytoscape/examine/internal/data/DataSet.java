package org.cytoscape.examine.internal.data;

import org.cytoscape.examine.internal.Constants;
import org.cytoscape.examine.internal.settings.NetworkSettings;
import org.cytoscape.examine.internal.signal.Variable;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Pseudograph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data set / module.
 */
public class DataSet {

    // Entire network of interest.
    public final Variable<SuperNetwork> superNetwork;

    // CyNode to HNode map.
    public final Variable<Map<CyNode, HNode>> nodeMap;

    // Minimum and maximum node score (for normalization).
    public final Variable<Double> minScore, maxScore;

    // Node sets by category.
    public final Variable<Map<String, HCategory>> categories;

    // CySet to HSet map.
    public final Variable<Map<CyNode, HSet>> setMap;

    /**
     * Construct data set from Cytoscape network.
     */
    public DataSet(
            final CyNetwork cyNetwork,
            final CyGroupManager groupManager,
            final NetworkSettings networkSettings) {

        // Fields.
        this.superNetwork = new Variable<SuperNetwork>(new SuperNetwork(null, new Pseudograph<HNode, DefaultEdge>(DefaultEdge.class)));
        this.nodeMap = new Variable<Map<CyNode, HNode>>(new HashMap<CyNode, HNode>());
        this.minScore = new Variable<Double>(0.);
        this.maxScore = new Variable<Double>(1.);
        this.categories = new Variable<Map<String, HCategory>>(new HashMap<String, HCategory>());
        this.setMap = new Variable<Map<CyNode, HSet>>(new HashMap<CyNode, HSet>());

        // Important variables.
        List<CyNode> regularNodes = new ArrayList<CyNode>();
        CyTable nodeTable = cyNetwork.getDefaultNodeTable();

        // Extract nodes to visualize
        Collection<CyRow> selectedRows = cyNetwork.getDefaultNodeTable().getMatchingRows(CyNetwork.SELECTED, true);
        for (CyRow row : selectedRows) {
            CyNode node = cyNetwork.getNode(row.get(CyNetwork.SUID, Long.class));
            if (node != null && !groupManager.isGroup(node, cyNetwork)) {
                regularNodes.add(node);
            }
        }

        // Regular and group node partition.
        List<CyNode> groupNodes = new ArrayList<CyNode>();
        for (CyGroup group : groupManager.getGroupSet(cyNetwork)) {
            groupNodes.add(group.getGroupNode());
        }

        // Wrap cyNetwork.
        UndirectedGraph<HNode, DefaultEdge> superGraph =
                new Pseudograph<HNode, DefaultEdge>(DefaultEdge.class);

        // Nodes.
        Map<CyNode, HNode> nM = new HashMap<CyNode, HNode>();
        for (CyNode cyNode : regularNodes) {
            CyRow row = nodeTable.getRow(cyNode.getSUID());

            if (row != null) {
                float score = 0;
                //if (scoreColumnName != null) {
                //  score = row.get(scoreColumnName, Double.class).floatValue();
                //}
                HNode hN = new HNode(
                        cyNode,
                        row,
                        row.get(CyNetwork.NAME, String.class),
                        row.get(networkSettings.getSelectedLabelColumnName(), String.class),
                        row.get(networkSettings.getSelectedURLColumnName(), String.class),
                        score);

                superGraph.addVertex(hN);
                nM.put(cyNode, hN);
            }
        }

        // Edges.
        for (CyEdge cyEdge : cyNetwork.getEdgeList()) {
            HNode sHN = nM.get(cyEdge.getSource());
            HNode tHN = nM.get(cyEdge.getTarget());

            // todo: only edges between selected nodes
            if (sHN != null && tHN != null)
                superGraph.addEdge(sHN, tHN);
        }

        SuperNetwork sN = new SuperNetwork(cyNetwork, superGraph);
        superNetwork.set(sN);

        // Wrap CyGroups.
        Map<String, HCategory> cs = new HashMap<String, HCategory>();

        // Determine category groups.
        double minScr = 1;
        double maxScr = 0;
        Map<CyNode, HSet> sM = new HashMap<CyNode, HSet>();
        for (CyNode gN : groupNodes) {
            CyRow row = nodeTable.getRow(gN.getSUID());

            // For every category node.
            if (row != null && row.get(CyNetwork.NAME, String.class).startsWith(Constants.CATEGORY_PREFIX)) {
                CyGroup catG = groupManager.getGroup(gN, cyNetwork);
                String catName = row.get(CyNetwork.NAME, String.class).substring(Constants.CATEGORY_PREFIX.length());

                /*A pointer to the column corresponding to the category*/
                CyColumn categoryColumn = null;
                boolean found = false;
                
                for (CyColumn entry : networkSettings.getAllGroupColumns()) {
                    if (catName.equals(entry.getName())) {
                        found = true;
                        categoryColumn = entry;
                        break;
                    }
                }

                if (!found) continue;

                System.out.println("Added category: " + catName);

                // Construct member sets.
                List<HSet> members = new ArrayList<HSet>();
                for (CyNode mCN : catG.getNodeList()) {
                    CyRow mRow = nodeTable.getRow(mCN.getSUID());

                    CyGroup mG = groupManager.getGroup(mCN, cyNetwork);
                    ArrayList<CyNode> mGNodes = new ArrayList<CyNode>(mG.getNodeList());
                    mGNodes.retainAll(regularNodes);

                    // don't show sets with no nodes
                    if (mGNodes.size() == 0) continue;

                    // Try to get symbol name, or fall back to id.
                    String name = mRow.get(networkSettings.getSelectedLabelColumnName(), String.class);
                    if (name == null || name.trim().isEmpty()) {
                        name = mRow.get(CyNetwork.NAME, String.class);
                    }

                    // Mapped member nodes.
                    List<HNode> mmNS = new ArrayList<HNode>();
                    for (CyNode mmN : mGNodes) {
                        HNode mmHN = nM.get(mmN);
                        if (mmHN != null)
                            mmNS.add(mmHN);
                    }

                    Double bScore = null;
                    if (networkSettings.getSelectedScoreColumnName() != null) {
                        bScore = mRow.get(networkSettings.getSelectedScoreColumnName(), Double.class);
                    }
                    else {
                    	System.out.println("NetworkSettings does nto contain a selected score column name!");
                    }
                    double score = bScore == null ? Double.NaN : bScore;

                    if (!Double.isNaN(score)) {
                        minScr = Math.min(minScr, score);
                        maxScr = Math.max(maxScr, score);
                    }

                    String url = mRow.get(networkSettings.getSelectedURLColumnName(), String.class);

                    HSet mHS = new HSet(mG, name, score, url, mmNS);
                    members.add(mHS);
                    sM.put(mCN, mHS);

                    // Register set with its members.
                    for (HNode mHN : mmNS) {
                        mHN.sets.add(mHS);
                    }
                }

                // Add category node.
                //System.out.println(catG == null ? "catG is null" : "catG is not null");
                //System.out.println(catName == null ? "catName is null" : "catName is not null");
                //System.out.println(members == null ? "members is null" : "members is not null");
                //System.out.println(groupColumnSizes == null ? "groupColumnSizes is null" : "groupColumnSizes is not null");
                //System.out.println(groupColumnSizes.get(idx) == null ? "groupColumnSizes.get(idx) is null" : "groupColumnSizes.get(idx) is not null");

                HCategory hC = new HCategory(catG, catName, members,networkSettings.getColumnSize(categoryColumn));
                cs.put(catName, hC);

                

            } // End for every category node.

            // Transfer node scores.
            this.minScore.set(minScr);
            this.maxScore.set(maxScr);
        }

        superNetwork.set(sN);
        nodeMap.set(nM);
        categories.set(cs);
    }

}
