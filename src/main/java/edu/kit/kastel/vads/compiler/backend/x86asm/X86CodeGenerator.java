package edu.kit.kastel.vads.compiler.backend.x86asm;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.backend.CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.aasm.AasmRegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;


public class X86CodeGenerator implements CodeGenerator {

    private static final String MAIN_ASM = """
        .global main
        .global _main
        .text
        main:
        call _main
        # move the return value into the first argument for the syscall
        movq %rax, %rdi
        # move the exit syscall number into rax
        movq $0x3C, %rax
        syscall
        _main:
        # your generated code here
        """;

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            RegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            builder.append(MAIN_ASM);
            generateForGraph(graph, builder, registers);
        }
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        // TODO: implement code generation
        switch (node) {
            case AddNode add -> addNode(builder, registers, add);
            case SubNode sub -> subNode(builder, registers, sub);
            case MulNode mul -> mulNode(builder, registers, mul);
            case DivNode div -> divNode(builder, registers, div);
            case ModNode mod -> modNode(builder, registers, mod);
            case ConstIntNode c -> constIntNode(builder, registers, c);
            case ReturnNode r -> returnNode(builder, registers, r);
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
            default ->
                throw new UnsupportedOperationException("Codegeneration for '" + node.toString() + "' is not implemented.");
        }
        builder.append("\n");
    }

    private static void returnNode(StringBuilder builder, Map<Node, Register> registers, ReturnNode r) {
        builder.repeat(" ", 2)
            .append("mov ")
            .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)))
            .append(",")
            .append(registers.get(r))
            .append("\n");

        builder.repeat(" ", 2)
            .append("ret");
    }

    private static void constIntNode(StringBuilder builder, Map<Node, Register> registers, ConstIntNode c) {
        builder.repeat(" ", 2)
            .append("mov $")
            .append(c.value())
            .append(",")
            .append(registers.get(c));
    }

    private static void addNode(StringBuilder builder, Map<Node, Register> registers, AddNode node) {
        builder.repeat(" ", 2)
            .append("add ")
            .append(registers.get(predecessorSkipProj(node, AddNode.LEFT)))
            .append(",")
            .append(registers.get(predecessorSkipProj(node, AddNode.RIGHT)));
    }

    private static void subNode(StringBuilder builder, Map<Node, Register> registers, SubNode node) {
        builder.repeat(" ", 2)
            .append("sub ")
            .append(registers.get(predecessorSkipProj(node, SubNode.LEFT)))
            .append(",")
            .append(registers.get(predecessorSkipProj(node, SubNode.RIGHT)));
    }

    private static void mulNode(StringBuilder builder, Map<Node, Register> registers, MulNode node) {
        builder.repeat(" ", 2)
            .append("imul ")
            .append(registers.get(predecessorSkipProj(node, MulNode.LEFT)))
            .append(",")
            .append(registers.get(predecessorSkipProj(node, MulNode.RIGHT)));
    }

    private static void divNode(StringBuilder builder, Map<Node, Register> registers, DivNode node) {
        builder.repeat(" ", 2)
            .append("mov ")
            .append(registers.get(predecessorSkipProj(node, DivNode.LEFT)))
            .append(",")
            .append(registers.get(node))
            .append("\n");

        builder.repeat(" ", 2)
            .append("idiv ")
            .append(registers.get(predecessorSkipProj(node, DivNode.RIGHT)));
    }

    private static void modNode(StringBuilder builder, Map<Node, Register> registers, ModNode node) {
        builder.repeat(" ", 2)
            .append("mov ")
            .append(registers.get(predecessorSkipProj(node, ModNode.LEFT)))
            .append(",")
            .append(registers.get(node))
            .append("\n");

        builder.repeat(" ", 2)
            .append("idiv ")
            .append(registers.get(predecessorSkipProj(node, ModNode.RIGHT)));
    }
}
