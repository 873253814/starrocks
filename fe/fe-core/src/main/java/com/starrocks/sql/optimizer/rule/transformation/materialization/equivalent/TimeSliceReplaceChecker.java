// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.starrocks.sql.optimizer.rule.transformation.materialization.equivalent;

import com.starrocks.catalog.FunctionSet;
import com.starrocks.catalog.PrimitiveType;
import com.starrocks.common.AnalysisException;
import com.starrocks.sql.optimizer.operator.scalar.CallOperator;
import com.starrocks.sql.optimizer.operator.scalar.ColumnRefOperator;
import com.starrocks.sql.optimizer.operator.scalar.ConstantOperator;
import com.starrocks.sql.optimizer.operator.scalar.ScalarOperator;
import com.starrocks.sql.optimizer.rewrite.ScalarOperatorFunctions;

public class TimeSliceReplaceChecker implements IRewriteEquivalent {
    public static final TimeSliceReplaceChecker INSTANCE = new TimeSliceReplaceChecker();

    private CallOperator mvTimeSlice;

    public TimeSliceReplaceChecker() {}

    public TimeSliceReplaceChecker(CallOperator mvTimeSlice) {
        this.mvTimeSlice = mvTimeSlice;
    }

    @Override
    public boolean isEquivalent(ScalarOperator operator) {
        if (mvTimeSlice == null) {
            return false;
        }
        if (!operator.isConstantRef()) {
            return false;
        }
        return isEquivalent(mvTimeSlice, (ConstantOperator) operator);
    }

    @Override
    public boolean isEquivalent(ScalarOperator op1, ConstantOperator op2) {
        if (!(op1 instanceof CallOperator)) {
            return false;
        }
        CallOperator func = (CallOperator) op1;
        if (!func.getFnName().equalsIgnoreCase(FunctionSet.TIME_SLICE)) {
            return false;
        }
        if (op2.getType().getPrimitiveType() != PrimitiveType.DATETIME) {
            return false;
        }
        if (!(func.getChild(0) instanceof ColumnRefOperator)) {
            return false;
        }
        try {
            ConstantOperator sliced = ScalarOperatorFunctions.timeSlice(
                    op2,
                    ((ConstantOperator) op1.getChild(1)),
                    ((ConstantOperator) op1.getChild(2)),
                    ((ConstantOperator) op1.getChild(3)));
            return sliced.equals(op2);
        } catch (AnalysisException e) {
            return false;
        }
    }
}
