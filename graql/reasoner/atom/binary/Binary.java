/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package grakn.core.graql.reasoner.atom.binary;

import com.google.common.collect.Sets;
import grakn.core.graql.reasoner.ReasoningContext;
import grakn.core.graql.reasoner.atom.Atom;
import grakn.core.graql.reasoner.atom.predicate.IdPredicate;
import grakn.core.graql.reasoner.atom.predicate.Predicate;
import grakn.core.graql.reasoner.atom.task.relate.BinarySemanticProcessor;
import grakn.core.graql.reasoner.atom.task.relate.SemanticProcessor;
import grakn.core.graql.reasoner.unifier.MultiUnifierImpl;
import grakn.core.graql.reasoner.unifier.UnifierType;
import grakn.core.kb.concept.api.ConceptId;
import grakn.core.kb.concept.api.Label;
import grakn.core.kb.concept.api.SchemaConcept;
import grakn.core.kb.graql.reasoner.ReasonerCheckedException;
import grakn.core.kb.graql.reasoner.query.ReasonerQuery;
import grakn.core.kb.graql.reasoner.unifier.Unifier;
import graql.lang.Graql;
import graql.lang.pattern.Pattern;
import graql.lang.property.IsaProperty;
import graql.lang.statement.Statement;
import graql.lang.statement.Variable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 *
 * <p>
 * Implementation for binary atoms with single id typePredicate for a schema concept. Binary atoms take the form:
 *
 * <>($varName, $predicateVariable), type($predicateVariable)
 *
 * </p>
 *
 *
 */
public abstract class Binary extends Atom {

    private final Variable predicateVariable;
    private final SemanticProcessor<Binary> semanticProcessor;

    private SchemaConcept type = null;
    private IdPredicate typePredicate = null;

    Binary(Variable varName, Statement pattern, ReasonerQuery reasonerQuery, @Nullable Label label,
           Variable predicateVariable, ReasoningContext ctx) {
        super(reasonerQuery, varName, pattern, label, ctx);
        this.predicateVariable = predicateVariable;
        this.semanticProcessor = new BinarySemanticProcessor(ctx.conceptManager());
    }

    public Variable getPredicateVariable() {
        return predicateVariable;
    }

    @Nullable
    public IdPredicate getTypePredicate(){
        if (typePredicate == null && getTypeLabel() != null) {
            ConceptId typeId = context().conceptManager().getSchemaConcept(getTypeLabel()).id();
            typePredicate = IdPredicate.create(new Statement(getPredicateVariable()).id(typeId.getValue()), getParentQuery());
        }
        return typePredicate;
    }

    public boolean isDirect(){
        return getPattern().getProperties(IsaProperty.class).findFirst()
                .map(IsaProperty::isExplicit).orElse(false);
    }

    @Nullable
    @Override
    public SchemaConcept getSchemaConcept(){
        if (type == null && getTypeLabel() != null) {
            SchemaConcept concept = context().conceptManager().getSchemaConcept(getTypeLabel());
            if (concept == null) throw ReasonerCheckedException.labelNotFound(getTypeLabel());
            type = concept;
        }
        return type;
    }

    @Override
    public void checkValid() {
        if (getTypePredicate() != null) getTypePredicate().checkValid();
    }

    @Override
    public boolean isAlphaEquivalent(Object obj) {
        if (!isBaseEquivalent(obj)) return false;
        Atom that = (Atom) obj;
        return !this.getMultiUnifier(that, UnifierType.EXACT).equals(MultiUnifierImpl.nonExistent());
    }

    @Override
    public boolean isStructurallyEquivalent(Object obj) {
        if (!isBaseEquivalent(obj)) return false;
        Atom that = (Atom) obj;
        return !this.getMultiUnifier(that, UnifierType.STRUCTURAL).equals(MultiUnifierImpl.nonExistent());
    }

    @Override
    public int alphaEquivalenceHashCode() {
        int hashCode = 1;
        hashCode = hashCode * 37 + (this.getTypeLabel() != null? this.getTypeLabel().hashCode() : 0);
        return hashCode;
    }

    @Override
    public int structuralEquivalenceHashCode() {
        return alphaEquivalenceHashCode();
    }

    boolean isBaseEquivalent(Object obj){
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        Binary that = (Binary) obj;
        return (this.isUserDefined() == that.isUserDefined())
                && (this.getPredicateVariable().isReturned() == that.getPredicateVariable().isReturned())
                && this.isDirect() == that.isDirect()
                && Objects.equals(this.getTypeLabel(), that.getTypeLabel());
    }

    @Override
    protected Pattern createCombinedPattern(){
        Set<Pattern> vars = Sets.newHashSet((Pattern) getPattern());
        if (getTypePredicate() != null) vars.add(getTypePredicate().getPattern());
        return Graql.and(vars);
    }

    @Override
    public Set<Variable> getVarNames() {
        Set<Variable> vars = new HashSet<>();
        if (getVarName().isReturned()) vars.add(getVarName());
        if (getPredicateVariable().isReturned()) vars.add(getPredicateVariable());
        return vars;
    }

    @Override
    public Stream<Predicate> getInnerPredicates(){
        return getTypePredicate() != null? Stream.of(getTypePredicate()) : Stream.empty();
    }

    @Override
    public Unifier getUnifier(Atom parentAtom, UnifierType unifierType) {
        return semanticProcessor.getUnifier(this, parentAtom, unifierType);
    }
}
