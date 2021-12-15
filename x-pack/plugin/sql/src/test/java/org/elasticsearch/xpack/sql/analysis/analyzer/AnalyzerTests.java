/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.analysis.analyzer;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ql.index.EsIndex;
import org.elasticsearch.xpack.ql.index.IndexResolution;
import org.elasticsearch.xpack.ql.plan.logical.LogicalPlan;
import org.elasticsearch.xpack.ql.plan.logical.Project;
import org.elasticsearch.xpack.sql.SqlTestUtils;
import org.elasticsearch.xpack.sql.expression.function.SqlFunctionRegistry;
import org.elasticsearch.xpack.sql.parser.SqlParser;
import org.elasticsearch.xpack.sql.plan.logical.Having;
import org.elasticsearch.xpack.sql.stats.Metrics;
import org.hamcrest.Matchers;

import static org.elasticsearch.xpack.sql.types.SqlTypesTests.loadMapping;

public class AnalyzerTests extends ESTestCase {

    private final SqlParser parser = new SqlParser();
    private final Analyzer analyzer = new Analyzer(
        SqlTestUtils.TEST_CFG,
        new SqlFunctionRegistry(),
        IndexResolution.valid(new EsIndex("test", loadMapping("mapping-basic.json"))),
        new Verifier(new Metrics())
    );

    private LogicalPlan analyze(String sql, boolean verify) {
        return analyzer.analyze(parser.createStatement(sql), verify);
    }

    public void testResolveRecursiveFilterRefs() {
        LogicalPlan plan = analyze("SELECT emp_no + 1 emp_no, emp_no + 1 e FROM test WHERE e > 0", false);

        assertThat(plan, Matchers.instanceOf(Project.class));
        assertTrue(plan.resolved());
    }

    public void testResolveAlternatingRecursiveFilterRefs() {
        // Queries like the following used to cause a StackOverflowError in ResolveFilterRefs.
        // see https://github.com/elastic/elasticsearch/issues/81577
        LogicalPlan plan = analyze(
            "SELECT MAX(emp_no) emp_no, MAX(emp_no) emp_no, AVG(emp_no) e FROM test WHERE e > 0 GROUP BY emp_no HAVING e > 0",
            false
        );

        assertThat(plan, Matchers.instanceOf(Having.class));
        assertTrue(plan.resolved());
    }

}
