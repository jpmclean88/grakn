/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.graql.internal.reasoner.atom.binary;

import ai.grakn.concept.ConceptId;
import ai.grakn.concept.SchemaConcept;
import ai.grakn.concept.Type;
import ai.grakn.exception.GraqlQueryException;
import ai.grakn.graql.Var;
import ai.grakn.graql.admin.PatternAdmin;
import ai.grakn.graql.admin.ReasonerQuery;
import ai.grakn.graql.admin.Unifier;
import ai.grakn.graql.admin.VarPatternAdmin;
import ai.grakn.graql.internal.pattern.Patterns;
import ai.grakn.graql.internal.reasoner.UnifierImpl;
import ai.grakn.graql.internal.reasoner.atom.Atom;
import ai.grakn.graql.internal.reasoner.atom.AtomicFactory;
import ai.grakn.graql.internal.reasoner.atom.predicate.IdPredicate;

import ai.grakn.graql.internal.reasoner.atom.predicate.Predicate;
import com.google.common.collect.Sets;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * <p>
 * Implementation for binary atoms with single id typePredicate for a schema concept. Binary atoms take the form:
 *
 * <>($varName, $predicateVariable), type($predicateVariable)
 *
 * </p>
 *
 * @author Kasper Piskorski
 *
 */
public abstract class Binary extends Atom {
    private final IdPredicate typePredicate;
    private final Var predicateVariable;
    private Type type = null;

    Binary(VarPatternAdmin pattern, Var predicateVar, @Nullable IdPredicate p, ReasonerQuery par) {
        super(pattern, par);
        this.predicateVariable = predicateVar;
        this.typePredicate = p;
    }

    Binary(Binary a) {
        super(a);
        this.predicateVariable = a.predicateVariable;
        this.typePredicate = a.getTypePredicate() != null ? (IdPredicate) AtomicFactory.create(a.getTypePredicate(), getParentQuery()) : null;
        this.type = a.type;
    }

    public Var getPredicateVariable() { return predicateVariable;}

    @Nullable
    public IdPredicate getTypePredicate() { return typePredicate;}

    @Nullable
    @Override
    public SchemaConcept getSchemaConcept(){
        if (type == null && getTypePredicate() != null) {
            type = getParentQuery().tx().getConcept(getTypeId()).asType();
        }
        return type;
    }

    @Override
    public ConceptId getTypeId(){ return typePredicate != null? typePredicate.getPredicate() : null;}

    @Override
    public int equivalenceHashCode() {
        int hashCode = 1;
        hashCode = hashCode * 37 + (this.getTypeId() != null? this.getTypeId().hashCode() : 0);
        return hashCode;
    }

    @Override
    public boolean isEquivalent(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        Binary a2 = (Binary) obj;
        return  (isUserDefined() == a2.isUserDefined())
                && Objects.equals(this.getTypeId(), a2.getTypeId())
                && hasEquivalentPredicatesWith(a2);
    }

    boolean hasEquivalentPredicatesWith(Binary atom) {
        //check if there is a substitution for varName
        IdPredicate thisVarPredicate = this.getIdPredicate(getVarName());
        IdPredicate varPredicate = atom.getIdPredicate(atom.getVarName());

        IdPredicate thisTypePredicate = this.getTypePredicate();
        IdPredicate typePredicate = atom.getTypePredicate();
        return ((thisVarPredicate == null && varPredicate == null || thisVarPredicate != null && thisVarPredicate.isEquivalent(varPredicate)))
                && (thisTypePredicate == null && typePredicate == null || thisTypePredicate != null && thisTypePredicate.isEquivalent(typePredicate));
    }

    @Override
    public PatternAdmin getCombinedPattern() {
        Set<VarPatternAdmin> vars = Sets.newHashSet(super.getPattern().asVarPattern());
        if (getTypePredicate() != null) vars.add(getTypePredicate().getPattern().asVarPattern());
        return Patterns.conjunction(vars);
    }

    @Override
    public void setParentQuery(ReasonerQuery q) {
        super.setParentQuery(q);
        if (typePredicate != null) typePredicate.setParentQuery(q);
    }

    @Override
    public Set<Var> getVarNames() {
        Set<Var> vars = new HashSet<>();
        if (getVarName().isUserDefinedName()) vars.add(getVarName());
        if (!predicateVariable.getValue().isEmpty()) vars.add(predicateVariable);
        return vars;
    }

    @Override
    public Stream<Predicate> getInnerPredicates(){
        return Stream.of(typePredicate);
    }

    @Override
    public Unifier getUnifier(Atom parentAtom) {
        if (!(parentAtom instanceof Binary)) {
            throw GraqlQueryException.unificationAtomIncompatibility();
        }

        Unifier unifier = new UnifierImpl();
        Var childPredicateVarName = this.getPredicateVariable();
        Var parentPredicateVarName = parentAtom.getPredicateVariable();

        if (parentAtom.getVarName().isUserDefinedName()){
            Var childVarName = this.getVarName();
            Var parentVarName = parentAtom.getVarName();
            if (!childVarName.equals(parentVarName)) {
                unifier.addMapping(childVarName, parentVarName);
            }
        }
        if (!childPredicateVarName.getValue().isEmpty()
                && !parentPredicateVarName.getValue().isEmpty()
                && !childPredicateVarName.equals(parentPredicateVarName)) {
            unifier.addMapping(childPredicateVarName, parentPredicateVarName);
        }
        return unifier;
    }
}
