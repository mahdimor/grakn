/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016-2018 Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/agpl.txt>.
 */

package ai.grakn.graql.internal.query.match;

import ai.grakn.graql.Match;
import ai.grakn.graql.answer.ConceptMap;
import ai.grakn.kb.internal.EmbeddedGraknTx;

import java.util.stream.Stream;

/**
 * "Order" modify that orders the underlying {@link Match}
 *
 * @author Grakn Warriors
 */
class MatchOrder extends MatchModifier {

    private final Ordering order;

    MatchOrder(AbstractMatch inner, Ordering order) {
        super(inner);
        this.order = order;
    }

    @Override
    public Stream<ConceptMap> stream(EmbeddedGraknTx<?> tx) {
        return order.orderStream(inner.stream(tx));
    }

    @Override
    protected String modifierString() {
        return " " + order.toString() + ";";
    }
}
