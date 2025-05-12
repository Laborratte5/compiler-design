package edu.kit.kastel.vads.compiler.backend.x86asm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.backend.CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.aasm.AasmRegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;


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
            .append("ret");
    }

    private static void constIntNode(StringBuilder builder, Map<Node, Register> registers, ConstIntNode c) {
        builder.repeat(" ", 2)
            .append("mov $")
            .append(c.value())
            .append(",")
            .append(registers.get(c));
    }

}
