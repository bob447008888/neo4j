/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_2.planner.logical

import org.neo4j.cypher.internal.compiler.v2_2.planner.QueryGraph
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.steps._
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.steps.solveOptionalMatches.OptionalSolver

case class PlanningStrategyConfiguration(
    leafPlanners: LeafPlannerList,
    applySelections: PlanTransformer[QueryGraph],
    projectAllEndpoints: PlanTransformer[QueryGraph],
    optionalSolvers: Seq[OptionalSolver],
    pickBestCandidate: CandidateSelector
  ) {

  val optionalMatchesSolver = solveOptionalMatches(optionalSolvers, pickBestCandidate)

  def kitInContext(implicit context: LogicalPlanningContext) = (qg: QueryGraph) =>
    PlanningStrategyKit(
      select = plan => applySelections(plan, qg)
    )
}

case class PlanningStrategyKit(select: LogicalPlan => LogicalPlan)

object PlanningStrategyConfiguration {
  val default = PlanningStrategyConfiguration(
    pickBestCandidate = pickBestPlan,
    applySelections = selectPatternPredicates(selectCovered, pickBestPlan),
    projectAllEndpoints = projectEndpoints.all,
    optionalSolvers = Seq(
      applyOptional,
      outerHashJoin
    ),
    leafPlanners = LeafPlannerList(
      argumentLeafPlanner,

      // MATCH n WHERE id(n) = {id} RETURN n
      idSeekLeafPlanner,

      // MATCH n WHERE n.prop = {val} RETURN n
      uniqueIndexSeekLeafPlanner,

      // MATCH n WHERE n.prop = {val} RETURN n
      indexSeekLeafPlanner,

      // MATCH (n:Person) RETURN n
      labelScanLeafPlanner,

      // MATCH n RETURN n
      allNodesLeafPlanner,

      // Legacy indices
      legacyHintLeafPlanner
    )
  )
}


