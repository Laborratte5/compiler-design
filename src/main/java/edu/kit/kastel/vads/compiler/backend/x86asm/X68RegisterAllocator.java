package edu.kit.kastel.vads.compiler.backend.x86asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;

public class X68RegisterAllocator implements RegisterAllocator {

    private class InterferenceGraph {

    }

    private final Map<Node, Set<Node>> temps;
    private int nextTemp;

    public X68RegisterAllocator() {
        temps = new HashMap<>();
        nextTemp = 0;
    }

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        analyseLive(graph);

        System.out.println("=====");
        printTemps();
        throw new UnsupportedOperationException();
        //InterferenceGraph interference = buildInterferenceGraph(temps);
        //return allocateRegisters(interference);
    }

    private void analyseLive(IrGraph graph) {
        boolean change = false;
        do {
            change = analyseLiveSinglePass(graph.endBlock(), new HashSet<>());
        } while (change);
    }

    private boolean analyseLiveSinglePass(Node node, Set<Node> visited) {
        boolean changed = false;

        if (needsRegister(node)) {
            // Get Live Out of node
            Set<Node> liveOut = temps.getOrDefault(node, Set.of());

            Set<Node> kill = getKillOf(node);
            Set<Node> gen = getGenOf(node);

            Set<Node> liveIn = new HashSet<>(liveOut);
            liveIn.removeAll(kill);
            liveIn.addAll(gen);

            Set<Node> old = temps.put(node, liveIn);
            changed |= old == null || !old.equals(liveIn);

            //if (changed) {
                System.out.println("Node " + node);
                System.out.println("Out: " + Arrays.toString(liveOut.toArray()));
                System.out.println("Kill: " + Arrays.toString(kill.toArray()));
                System.out.println("Gen: " + Arrays.toString(gen.toArray()));
                System.out.println("In: " + Arrays.toString(liveIn.toArray()));
                System.out.println();
                //printTemps();
                //System.out.println();
            //}

        }

        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                changed |= analyseLiveSinglePass(predecessor, visited);
            }
        }

        return changed;
    }

    private Set<Node> getSuccessors(Node node) {
        return node.graph().successors(node);
    }

    private InterferenceGraph buildInterferenceGraph(Map<Node, Set<Node>> temps) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildInterferenceGraph'");
    }

    private Map<Node, Register> allocateRegisters(InterferenceGraph graph) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'allocateRegisters'");
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block);
    }

    private Set<Node> getKillOf(Node node) {
        return switch (node) {
            case BinaryOperationNode bin -> Set.of(bin);
            case ConstIntNode c -> Set.of(c);
            case ReturnNode _ -> Set.of();
            default -> throw new UnsupportedOperationException("No 'kill' set for " + node + "implemented.");
        };
    }

    private Set<Node> getGenOf(Node node) {
        return switch (node) {
            // TODO special case div/mod
            case BinaryOperationNode bin -> Set.of(bin.predecessor(BinaryOperationNode.LEFT), bin.predecessor(BinaryOperationNode.RIGHT));
            case ConstIntNode _ -> Set.of();
            case ReturnNode ret -> Set.of(ret.predecessor(ReturnNode.RESULT));
            default -> throw new UnsupportedOperationException("No 'kill' set for " + node + "implemented.");
        };
    }

    private void printTemps() {

        for (Entry<Node, Set<Node>> e : temps.entrySet()) {
            //System.out.println(e.getValue().size());
            System.out.println(e.getKey() + ": " + e.getValue().stream().map(r -> String.valueOf(r.hashCode())).collect(Collectors.joining(", ")));
        }

    }

}
