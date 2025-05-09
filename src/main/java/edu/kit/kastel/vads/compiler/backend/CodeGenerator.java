package edu.kit.kastel.vads.compiler.backend;

import java.util.List;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public interface CodeGenerator {

    public String generateCode(List<IrGraph> program);

}
