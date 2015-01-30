/**
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.utility;

import com.google.common.base.Optional;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.constraint.Max;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.exception.ConstraintsViolatedException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.NamingException;

/**
 * Tests for the <code>OvalBuilder</code> class. Note, the purpose of this class
 * is not to test Oval, but the OvalBuilder and serve as illustration with a few
 * simple use cases.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class OvalBuilderTest {

    @Test
    public void testSuccess() {
        final int intValue = 1;
        final int rangeIntValue = 50;
        final TestBean testBean = TestBean.Builder.newInstance()
                .setInt(Integer.valueOf(intValue))
                .setRangeInt(Integer.valueOf(rangeIntValue))
                .build();
        Assert.assertNotNull(testBean);
        Assert.assertEquals(intValue, testBean.getInt());
        Assert.assertTrue(testBean.getRangeInt().isPresent());
        Assert.assertEquals(rangeIntValue, testBean.getRangeInt().get().intValue());
    }

    @Test
    public void testSuccessRangeIntOptional() {
        final int intValue = 1;
        final TestBean testBean = TestBean.Builder.newInstance()
                .setInt(Integer.valueOf(intValue))
                .build();
        Assert.assertNotNull(testBean);
        Assert.assertEquals(intValue, testBean.getInt());
        Assert.assertFalse(testBean.getRangeInt().isPresent());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeanNoDefaultConstructor() {
        NoBuilderConstructorBean.Builder.newInstance().build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBeanConstructorFailureWithException() {
        BadThrowingBean.Builder.newInstance().build();
    }

    @Test(expected = RuntimeException.class)
    public void testBeanConstructorFailureWithRuntimeException() {
        BadRuntimeThrowingBean.Builder.newInstance().build();
    }

    @Test
    public void testFailureImplicitNull() {
        try {
            TestBean.Builder.newInstance()
                    .setRangeInt(Integer.valueOf(50))
                    .build();
            Assert.fail("Expected exception not thrown");
        } catch (final ConstraintsViolatedException e) {
            Assert.assertNotNull(e.getConstraintViolations());
            Assert.assertEquals(1, e.getConstraintViolations().length);
            final ConstraintViolation cv = e.getConstraintViolations()[0];
            Assert.assertEquals("net.sf.oval.constraint.NotNullCheck", cv.getCheckName());
            Assert.assertThat(cv.getContext().toString(),
                    Matchers.containsString("OvalBuilderTest$TestBean$Builder._int"));
            Assert.assertNull(cv.getInvalidValue());
        }
    }

    @Test
    public void testFailureExplicitNull() {
        try {
            TestBean.Builder.newInstance()
                    .setInt(null)
                    .setRangeInt(Integer.valueOf(50))
                    .build();
            Assert.fail("Expected exception not thrown");
        } catch (final ConstraintsViolatedException e) {
            Assert.assertNotNull(e.getConstraintViolations());
            Assert.assertEquals(1, e.getConstraintViolations().length);
            final ConstraintViolation cv = e.getConstraintViolations()[0];
            Assert.assertEquals("net.sf.oval.constraint.NotNullCheck", cv.getCheckName());
            Assert.assertThat(cv.getContext().toString(),
                    Matchers.containsString("OvalBuilderTest$TestBean$Builder._int"));
            Assert.assertNull(cv.getInvalidValue());
        }
    }

    @Test
    public void testFailureMinViolation() {
        try {
            TestBean.Builder.newInstance()
                    .setInt(Integer.valueOf(0))
                    .setRangeInt(Integer.valueOf(-1))
                    .build();
            Assert.fail("Expected exception not thrown");
        } catch (final ConstraintsViolatedException e) {
            Assert.assertNotNull(e.getConstraintViolations());
            Assert.assertEquals(1, e.getConstraintViolations().length);
            final ConstraintViolation cv = e.getConstraintViolations()[0];
            Assert.assertEquals("net.sf.oval.constraint.MinCheck", cv.getCheckName());
            Assert.assertThat(cv.getContext().toString(),
                    Matchers.containsString("OvalBuilderTest$TestBean$Builder._rangeInt"));
            Assert.assertEquals(Integer.valueOf(-1), cv.getInvalidValue());
        }
    }

    @Test
    public void testFailureMaxViolation() {
        try {
            TestBean.Builder.newInstance()
                    .setInt(Integer.valueOf(0))
                    .setRangeInt(Integer.valueOf(101))
                    .build();
            Assert.fail("Expected exception not thrown");
        } catch (final ConstraintsViolatedException e) {
            Assert.assertNotNull(e.getConstraintViolations());
            Assert.assertEquals(1, e.getConstraintViolations().length);
            final ConstraintViolation cv = e.getConstraintViolations()[0];
            Assert.assertEquals("net.sf.oval.constraint.MaxCheck", cv.getCheckName());
            Assert.assertThat(cv.getContext().toString(),
                    Matchers.containsString("OvalBuilderTest$TestBean$Builder._rangeInt"));
            Assert.assertEquals(Integer.valueOf(101), cv.getInvalidValue());
        }
    }

    @Test
    public void testClone() {
        final TestBean beanA = new TestBean.Builder()
                .setInt(Integer.valueOf(0))
                .setRangeInt(Integer.valueOf(1))
                .build();
        Assert.assertEquals(0, beanA.getInt());
        Assert.assertEquals(Optional.of(1), beanA.getRangeInt());

        final TestBean beanB = OvalBuilder.<TestBean, TestBean.Builder>clone(beanA)
                .build();
        Assert.assertNotSame(beanA, beanB);
        Assert.assertEquals(0, beanB.getInt());
        Assert.assertEquals(Optional.of(1), beanB.getRangeInt());
    }

    @Test
    public void testCloneWithBuilder() {
        final TestBean beanA = new TestBean.Builder()
                .setInt(Integer.valueOf(0))
                .setRangeInt(Integer.valueOf(1))
                .build();
        Assert.assertEquals(0, beanA.getInt());
        Assert.assertEquals(Optional.of(1), beanA.getRangeInt());

        final TestBean beanB = OvalBuilder.<TestBean, TestBean.Builder>clone(beanA, new TestBean.Builder())
                .build();
        Assert.assertNotSame(beanA, beanB);
        Assert.assertEquals(0, beanB.getInt());
        Assert.assertEquals(Optional.of(1), beanB.getRangeInt());
    }

    private static final class TestBean {

        public int getInt() {
            return _int;
        }

        public Optional<Integer> getRangeInt() {
            return _rangeInt;
        }

        private TestBean(final Builder builder) {
            _int = builder._int.intValue();
            _rangeInt = Optional.fromNullable(builder._rangeInt);
        }

        private final int _int;
        private final Optional<Integer> _rangeInt;

        private static final class Builder extends OvalBuilder<TestBean> {

            public static Builder newInstance() {
                return new Builder();
            }

            public Builder setInt(final Integer value) {
                _int = value;
                return this;
            }

            public Builder setRangeInt(final Integer value) {
                _rangeInt = value;
                return this;
            }

            private Builder() {
                super(TestBean.class);
            }

            @NotNull
            private Integer _int;

            @Max(value = 100)
            @Min(value = 0)
            private Integer _rangeInt;
        }
    }

    private static final class BadThrowingBean {

        private BadThrowingBean(final Builder builder) throws NamingException {
            throw new NamingException();
        }

        private static final class Builder extends OvalBuilder<BadThrowingBean> {

            public static Builder newInstance() {
                return new Builder();
            }

            private Builder() {
                super(BadThrowingBean.class);
            }
        }
    }

    private static final class BadRuntimeThrowingBean {

        private BadRuntimeThrowingBean(final Builder builder) {
            throw new NullPointerException();
        }

        private static final class Builder extends OvalBuilder<BadRuntimeThrowingBean> {

            public static Builder newInstance() {
                return new Builder();
            }

            private Builder() {
                super(BadRuntimeThrowingBean.class);
            }
        }
    }

    private static final class NoBuilderConstructorBean {

        private NoBuilderConstructorBean() {}

        private static final class Builder extends OvalBuilder<NoBuilderConstructorBean> {

            public static Builder newInstance() {
                return new Builder();
            }

            private Builder() {
                super(NoBuilderConstructorBean.class);
            }
        }
    }
}
