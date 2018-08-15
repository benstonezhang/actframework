package act.controller;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.osgl.http.H.Format.HTML;

import act.Act;
import act.app.ActionContext;
import act.conf.AppConfigKey;
import act.controller.meta.HandlerMethodMetaInfo;
import act.data.Versioned;
import act.route.Router;
import act.util.$$;
import act.util.FastJsonIterable;
import act.util.JsonUtilConfig.JsonWriter;
import act.util.PropertySpec;
import act.view.*;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
import org.osgl.storage.ISObject;
import org.osgl.util.*;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.Map;
import javax.inject.Inject;

/**
 * Mark a class as Controller, which contains at least one of the following:
 * <ul>
 * <li>Action handler method</li>
 * <li>Any one of Before/After/Exception/Finally interceptor</li>
 * </ul>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Controller {

    /**
     * Indicate the context path for all action methods declared
     * in this controller.
     * <p/>
     * <p>Default value: "{@code /}"</p>
     *
     * @return the controller context path
     */
    String value() default "/";

    /**
     * Specify the port(s) this controller's action method shall be
     * routed from.
     *
     * @return the port name
     * @see AppConfigKey#NAMED_PORTS
     */
    String[] port() default {};

    /**
     * Provides utilities for controller action methods to emit rendering results
     */
    class Util {

        public static final Ok OK = Ok.get();
        public static final Created CREATED = Created.INSTANCE;
        public static final Result CREATED_JSON = new Result(H.Status.CREATED, "{\"message\": \"Created\"}") {
        };
        public static final Result CREATED_XML = new Result(H.Status.CREATED, "<?xml version=\"1.0\" ?><message>Created</message>") {
        };
        public static final Result OK_JSON = new Result(H.Status.OK, "{\"message\": \"Okay\"}") {
        };
        public static final Result OK_XML = new Result(H.Status.OK, "<?xml version=\"1.0\" ?><message>Okay</message>") {
        };
        public static final NoContent NO_CONTENT = NoContent.get();

        /**
         * Returns an {@link Ok} result
         */
        public static Result ok() {
            H.Format accept = ActionContext.current().accept();
            if (H.Format.JSON == accept) {
                return OK_JSON;
            } else if (H.Format.XML == accept) {
                return OK_XML;
            }
            return OK;
        }

        /**
         * Returns a {@link Created} result
         *
         * @param resourceGetUrl
         *         the URL to access the new resource been created
         * @return the result as described
         */
        public static Created created(String resourceGetUrl) {
            return Created.withLocation(resourceGetUrl);
        }

        /**
         * Return a {@link Created} result
         *
         * @return the result as described
         */
        public static Created created() {
            return Created.INSTANCE;
        }

        public static NotModified notModified() {
            return NotModified.get();
        }

        public static NotModified notModified(String etag, Object... args) {
            return NotModified.of(etag, args);
        }

        /**
         * Returns a {@link Accepted} result
         *
         * @param statusMonitorUrl
         *         the URL to check the request process status
         * @return the result as described
         */
        public static Result accepted(String statusMonitorUrl) {
            return new Accepted(statusMonitorUrl);
        }

        public static Result notAcceptable() {
            return NotAcceptable.get();
        }

        public static Result notAcceptable(String msg, Object... args) {
            return NotAcceptable.of(msg, args);
        }

        /**
         * Returns an {@link NotFound} result
         */
        public static Result notFound() {
            return ActNotFound.create();
        }

        /**
         * Returns an {@link NotFound} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static Result notFound(String msg, Object... args) {
            return ActNotFound.create(msg, args);
        }

        /**
         * Throws out an {@link NotFound} result if the object specified is
         * {@code null}
         *
         * @param o
         *         the object to be evaluated
         */
        public static <T> T notFoundIfNull(T o) {
            if (null == o) {
                throw ActNotFound.create();
            }
            return o;
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the object specified is {@code null}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param o
         *         the object to be evaluated
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static <T> T notFoundIfNull(T o, String msg, Object... args) {
            if (null == o) {
                throw ActNotFound.create(msg, args);
            }
            return o;
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code true}
         * {@code null}
         *
         * @param test
         *         the boolean expression to be evaluated
         */
        public static void notFoundIf(boolean test) {
            if (test) {
                throw ActNotFound.create();
            }
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code true}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param test
         *         the boolean expression
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static void notFoundIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActNotFound.create(msg, args);
            }
        }

        /**
         * Throws out an {@link NotFound} result if the boolean expression specified
         * is {@code false}
         * {@code null}
         *
         * @param test
         *         the boolean expression to be evaluated
         */
        public static void notFoundIfNot(boolean test) {
            notFoundIf(!test);
        }

        /**
         * Throws out an {@link NotFound} result with custom message template and
         * arguments if the expression specified is {@code false}. The final message is
         * rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param test
         *         the boolean expression
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static void notFoundIfNot(boolean test, String msg, Object... args) {
            notFoundIf(!test, msg, args);
        }

        /**
         * Returns a {@link BadRequest} instance.
         *
         * @return a bad request response
         */
        public static BadRequest badRequest() {
            return ActBadRequest.create();
        }

        /**
         * Returns a {@link BadRequest} instance along with error message.
         *
         * @param msg
         *         the message pattern
         * @param args
         *         the message argument
         * @return a bad request with error message
         */
        public static BadRequest badRequest(String msg, Object... args) {
            return ActBadRequest.create(msg, args);
        }

        /**
         * Throws out a {@link BadRequest} if test evaluated to `true`.
         *
         * @param test
         *         the test
         */
        public static void badRequestIf(boolean test) {
            if (test) {
                throw ActBadRequest.create();
            }
        }

        /**
         * Throws out a {@link BadRequest} with error message if test evaluated to `true`.
         *
         * @param test
         *         the test
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         */
        public static void badRequestIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActBadRequest.create(msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} if test string {@link S#blank(String)}.
         *
         * @param test
         *         the string to be checked
         */
        public static void badRequestIfBlank(String test) {
            if (S.blank(test)) {
                throw ActBadRequest.create();
            }
        }

        /**
         * Throws out a {@link BadRequest} with error message if test string is blank
         *
         * @param test
         *         the string to be checked
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message argument
         */
        public static void badRequestIfBlank(String test, String msg, Object... args) {
            if (S.blank(test)) {
                throw ActBadRequest.create(msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} if test object is `null`
         *
         * @param test
         *         the object to be checked
         */
        public static void badRequestIfNull(Object test) {
            if (null == test) {
                throw ActBadRequest.create();
            }
        }

        /**
         * Throws out a {@link BadRequest} with error message if test object is null
         *
         * @param test
         *         the object to be checked
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message argument
         */
        public static void badRequestIfNull(Object test, String msg, Object... args) {
            if (null == test) {
                throw ActBadRequest.create(msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} if test evaluated to `false`.
         *
         * @param test
         *         the test
         */
        public static void badRequestIfNot(boolean test) {
            if (!test) {
                throw ActBadRequest.create();
            }
        }

        /**
         * Throws out a {@link BadRequest} with error message if test evaluated to `false`.
         *
         * @param test
         *         the test
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         */
        public static void badRequestIfNot(boolean test, String msg, Object... args) {
            badRequestIf(!test, msg, args);
        }

        /**
         * Returns a {@link BadRequest} instance with error code specified.
         *
         * @param code
         *         the error code
         * @return the bad request instance
         */
        public static BadRequest badRequest(int code) {
            return ActBadRequest.create(code);
        }

        /**
         * Returns a {@link BadRequest} instance with error code and message specified.
         *
         * @param code
         *         the error code
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @return the bad request
         */
        public static BadRequest badRequest(int code, String msg, Object... args) {
            return ActBadRequest.create(code, msg, args);
        }

        /**
         * Throws out a {@link BadRequest} with error code if `test` evaluated to `true`.
         *
         * @param test
         *         the test
         * @param code
         *         the error code
         */
        public static void badRequestIf(boolean test, int code) {
            if (test) {
                throw ActBadRequest.create(code);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code and message if `test` evaluated to `true`.
         *
         * @param test
         *         the test
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void badRequestIf(boolean test, int code, String msg, Object... args) {
            if (test) {
                throw ActBadRequest.create(code, msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code if `test` string {@link S#blank(String) is blank}.
         *
         * @param test
         *         the test string
         * @param code
         *         the error code
         */
        public static void badRequestIfBlank(String test, int code) {
            if (S.blank(test)) {
                throw ActBadRequest.create(code);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code and message if `test` string {@link S#blank(String) is blank}.
         *
         * @param test
         *         the test string
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void badRequestIfBlank(String test, int code, String msg, Object... args) {
            if (S.blank(test)) {
                throw ActBadRequest.create(code, msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code if `test` is `null`.
         *
         * @param test
         *         the test object
         * @param code
         *         the error code
         */
        public static void badRequestIfNull(Object test, int code) {
            if (null == test) {
                throw ActBadRequest.create(code);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code and message if `test` is `null`.
         *
         * @param test
         *         the test object
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void badRequestIfNull(Object test, int code, String msg, Object... args) {
            if (null == test) {
                throw ActBadRequest.create(code, msg, args);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code if `test` is `false`.
         *
         * @param test
         *         the test object
         * @param code
         *         the error code
         */
        public static void badRequestIfNot(boolean test, int code) {
            if (!test) {
                throw ActBadRequest.create(code);
            }
        }

        /**
         * Throws out a {@link BadRequest} with error code and message if `test` is `false`.
         *
         * @param test
         *         the test object
         * @param msg
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void badRequestIfNot(boolean test, int code, String msg, Object... args) {
            badRequestIf(!test, code, msg, args);
        }

        /**
         * Returns a {@link Conflict} instance.
         *
         * @return a 409 Conflict result
         */
        public static Conflict conflict() {
            return ActConflict.create();
        }

        /**
         * Returns a {@link Conflict} instance with error message.
         *
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @return a 409 Conflict result
         */
        public static Conflict conflict(String message, Object... args) {
            return ActConflict.create(message, args);
        }

        /**
         * Throws out a {@link Conflict} if `test` is `true`.
         *
         * @param test
         *         the test
         */
        public static void conflictIf(boolean test) {
            if (test) {
                throw ActConflict.create();
            }
        }

        /**
         * Throws out a {@link Conflict} with error message specified if `test` is `true`.
         *
         * @param test
         *         the test
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         */
        public static void conflictIf(boolean test, String message, Object... args) {
            if (test) {
                throw ActConflict.create(message, args);
            }
        }

        /**
         * Throws out a {@link Conflict} if `test` is `false`.
         *
         * @param test
         *         the test
         */
        public static void conflictIfNot(boolean test) {
            conflictIf(!test);
        }

        /**
         * Throws out a {@link Conflict} with error message specified if `test` is `false`.
         *
         * @param test
         *         the test
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         */
        public static void conflictIfNot(boolean test, String message, Object... args) {
            conflictIf(!test, message, args);
        }

        /**
         * Returns a {@link Conflict} instance with error code.
         *
         * @param code
         *         the error message code
         * @return a 409 Conflict result
         */
        public static Conflict conflict(int code) {
            return ActConflict.create(code);
        }

        /**
         * Returns a {@link Conflict} instance with error code and message specified.
         *
         * @param code
         *         the error message code
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @return a 409 Conflict result
         */
        public static Conflict conflict(int code, String message, Object... args) {
            return ActConflict.create(code, message, args);
        }

        /**
         * Throws out a {@link Conflict} with error code if `test` is `true`.
         *
         * @param test
         *         the test
         * @param code
         *         the error code
         */
        public static void conflictIf(boolean test, int code) {
            if (test) {
                throw ActConflict.create(code);
            }
        }

        /**
         * Throws out a {@link Conflict} with error code and message if `test` is `true`.
         *
         * @param test
         *         the test
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void conflictIf(boolean test, int code, String message, Object... args) {
            if (test) {
                throw ActConflict.create(code, message, args);
            }
        }

        /**
         * Throws out a {@link Conflict} with error code if `test` is `false`.
         *
         * @param test
         *         the test
         * @param code
         *         the error code
         */
        public static void conflictIfNot(boolean test, int code) {
            conflictIf(!test, code);
        }

        /**
         * Throws out a {@link Conflict} with error code and message if `test` is `false`.
         *
         * @param test
         *         the test
         * @param message
         *         the error message pattern
         * @param args
         *         the error message arguments
         * @param code
         *         the error code
         */
        public static void conflictIfNot(boolean test, int code, String message, Object... args) {
            conflictIf(!test, code, message, args);
        }

        /**
         * Returns a {@link Unauthorized} instance.
         *
         * @return a 401 Unauthorized result
         */
        public static Unauthorized unauthorized() {
            return ActUnauthorized.create();
        }

        /**
         * Returns a {@link Unauthorized} instance with `realm` specified.
         *
         * @param realm
         *         the realm of the `Unauthorized` response
         * @return a 401 Unauthorized result
         */
        public static Unauthorized unauthorized(String realm) {
            return ActUnauthorized.create(realm);
        }

        /**
         * Returns a {@link Unauthorized} instance with `realm` and `digest` specified.
         *
         * @param realm
         *         the realm of the `Unauthorized` response
         * @param digest
         *         whether apply digest
         * @return a 401 Unauthorized result
         */
        public static Unauthorized unauthorized(String realm, boolean digest) {
            return ActUnauthorized.create(realm, digest);
        }

        /**
         * Throws out an {@link Unauthorized} instance if `test ` is `true`.
         *
         * @param test
         *         the test
         */
        public static void unauthorizedIf(boolean test) {
            if (test) {
                throw ActUnauthorized.create();
            }
        }

        /**
         * Throws out an {@link Unauthorized} instance with `realm` specified if `test ` is `true`.
         *
         * @param test
         *         the test
         * @param realm
         *         the realm
         */
        public static void unauthorizedIf(boolean test, String realm) {
            if (test) {
                throw ActUnauthorized.create(realm);
            }
        }

        /**
         * Throws out an {@link Unauthorized} instance with `realm` and `digest` specified if `test ` is `true`.
         *
         * @param test
         *         the test
         * @param realm
         *         the realm
         * @param digest
         *         whether apply digest
         */
        public static void unauthorizedIf(boolean test, String realm, boolean digest) {
            if (test) {
                throw ActUnauthorized.create(realm, digest);
            }
        }

        /**
         * Throws out an {@link Unauthorized} instance if `test ` is `false`.
         *
         * @param test
         *         the test
         */
        public static void unauthorizedIfNot(boolean test) {
            unauthorizedIf(!test);
        }

        /**
         * Throws out an {@link Unauthorized} instance with `realm` specified if `test ` is `false`.
         *
         * @param test
         *         the test
         * @param realm
         *         the realm
         */
        public static void unauthorizedIfNot(boolean test, String realm) {
            unauthorizedIf(!test, realm);
        }

        /**
         * Throws out an {@link Unauthorized} instance with `realm` and `digest` specified if `test ` is `false`.
         *
         * @param test
         *         the test
         * @param realm
         *         the realm
         * @param digest
         *         whether apply digest
         */
        public static void unauthorizedIfNot(boolean test, String realm, boolean digest) {
            unauthorizedIf(!test, realm, digest);
        }

        /**
         * Returns a {@link Forbidden} result
         */
        public static Forbidden forbidden() {
            return ActForbidden.create();
        }

        /**
         * Returns a {@link Forbidden} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static Forbidden forbidden(String msg, Object... args) {
            return ActForbidden.create(msg, args);
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code true}
         *
         * @param test
         *         the test condition
         */
        public static void forbiddenIf(boolean test) {
            if (test) {
                throw ActForbidden.create();
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test
         *         the test condition
         */
        public static void forbiddenIfNot(boolean test) {
            forbiddenIf(!test);
        }

        /**
         * Throws a {@link Forbidden} result if test condition is {@code true}
         *
         * @param test
         *         the test condition
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         */
        public static void forbiddenIf(boolean test, String msg, Object... args) {
            if (test) {
                throw ActForbidden.create(msg, args);
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test
         *         the test condition
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         */
        public static void forbiddenIfNot(boolean test, String msg, Object... args) {
            forbiddenIf(!test, msg, args);
        }


        /**
         * Returns a {@link Forbidden} result
         */
        public static Forbidden forbidden(int code) {
            return ActForbidden.create(code);
        }

        /**
         * Returns a {@link Forbidden} result with custom message
         * template and arguments. The final message is rendered with
         * the template and arguments using {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message template
         * @param args
         *         the message argument
         */
        public static Forbidden forbidden(String msg, int code, Object... args) {
            return ActForbidden.create(msg, args);
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code true}
         *
         * @param test
         *         the test condition
         */
        public static void forbiddenIf(boolean test, int code) {
            if (test) {
                throw ActForbidden.create(code);
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test
         *         the test condition
         */
        public static void forbiddenIfNot(boolean test, int code) {
            forbiddenIf(!test, code);
        }

        /**
         * Throws a {@link Forbidden} result if test condition is {@code true}
         *
         * @param test
         *         the test condition
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         */
        public static void forbiddenIf(boolean test, int code, String msg, Object... args) {
            if (test) {
                throw ActForbidden.create(code, msg, args);
            }
        }

        /**
         * Throws a {@link Forbidden} result if the test condition is {@code false}
         *
         * @param test
         *         the test condition
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         */
        public static void forbiddenIfNot(boolean test, int code, String msg, Object... args) {
            forbiddenIf(!test, code, msg, args);
        }

        public static void forward(String url) {
            ActionContext ctx = ActionContext.current();
            ctx.forward(url);
        }

        public static Redirect redirect(String url, Object... args) {
            return Redirect.of(redirectUrl(url, args));
        }

        public static Redirect redirect(String url, Map reverseRoutingArguments) {
            return Redirect.of(redirectUrl(url, reverseRoutingArguments));
        }

        public static void redirectIf(boolean test, String url, Object... args) {
            if (test) {
                throw redirect(url, args);
            }
        }

        public static void redirectIfNot(boolean test, String url, Object... args) {
            redirectIf(!test, url, args);
        }

        public static void redirectIf(boolean test, String url, Map reverseRoutingArguments) {
            if (test) {
                throw redirect(url, reverseRoutingArguments);
            }
        }

        public static void redirectIfNot(boolean test, String url, Map reverseRoutingArguments) {
            redirectIf(!test, url, reverseRoutingArguments);
        }

        public static MovedPermanently moved(String url, Object... args) {
            return MovedPermanently.of(redirectUrl(url, args));
        }

        public static MovedPermanently moved(String url, Map reverseRoutingArguments) {
            return MovedPermanently.of(redirectUrl(url, reverseRoutingArguments));
        }

        public static Found found(String url, Object... args) {
            return Found.of(redirectUrl(url, args));
        }

        public static Found found(String url, Map reverseRoutingArguments) {
            return Found.of(redirectUrl(url, reverseRoutingArguments));
        }

        public static void foundIf(boolean test, String url, Object... args) {
            if (test) {
                throw found(url, args);
            }
        }

        public static void foundIfNot(boolean test, String url, Object... args) {
            foundIf(!test, url, args);
        }

        public static SeeOther seeOther(String url, Object... args) {
            return SeeOther.of(redirectUrl(url, args));
        }

        public static SeeOther seeOther(String url, Map reverseRoutingArguments) {
            return SeeOther.of(redirectUrl(url, reverseRoutingArguments));
        }

        public static void seeOtherIf(boolean test, String url, Object... args) {
            if (test) {
                throw seeOther(url, args);
            }
        }

        public static void seeOtherIfNot(boolean test, String url, Object... args) {
            seeOtherIf(!test, url, args);
        }

        public static TemporaryRedirect temporaryRedirect(String url, Object... args) {
            return TemporaryRedirect.of(redirectUrl(url, args));
        }

        public static TemporaryRedirect temporaryRedirect(String url, Map reverseRoutingArguments) {
            return TemporaryRedirect.of(redirectUrl(url, reverseRoutingArguments));
        }

        public static PermanentRedirect permanentRedirect(String url, Object... args) {
            return PermanentRedirect.of(redirectUrl(url, args));
        }

        public static PermanentRedirect permanentRedirect(String url, Map reverseRoutingArguments) {
            return PermanentRedirect.of(redirectUrl(url, reverseRoutingArguments));
        }

        private static String redirectUrl(String url, Object... args) {
            url = S.fmt(url, args);
            if (url.contains(".") || url.contains("(")) {
                String inferFullActionPath = Router.inferFullActionPath(url);
                if (inferFullActionPath != url) {
                    url = ActionContext.current().router().reverseRoute(url);
                }
            } else {
                if (!url.startsWith("/")) {
                    ActionContext context = ActionContext.current();
                    String urlContext = context.urlContext();
                    if (S.notBlank(urlContext)) {
                        url = S.pathConcat(urlContext, '/', url);
                    }
                }
            }
            return url;
        }

        private static String redirectUrl(String url, Map reverseRoutingArguments) {
            url = Router.inferFullActionPath(url);
            url = ActionContext.current().router().reverseRoute(url, reverseRoutingArguments);
            return url;
        }


        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         */
        public static RenderText text(String msg, Object... args) {
            return RenderText.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #text(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderText renderText(String msg, Object... args) {
            return text(msg, args);
        }

        /**
         * Returns a {@link RenderText} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderHtml html(String msg, Object... args) {
            return RenderHtml.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #html(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderHtml renderHtml(String msg, Object args) {
            return html(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderJSON json(String msg, Object... args) {
            return RenderJSON.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #json(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderJSON renderJson(String msg, Object... args) {
            return json(msg, args);
        }

        /**
         * Returns a {@link RenderJSON} result with any object. This method will
         * call underline JSON serializer to transform the object into a JSON string
         *
         * @param data
         *         the data to be rendered as JSON string
         * @return the result
         */
        public static RenderJSON json(Object data) {
            return RenderJSON.of(successStatus(), data);
        }

        /**
         * Alias of {@link #json(Object)}
         *
         * @param data
         *         the data to be rendered as JSON string
         * @return the result
         */
        public static RenderJSON renderJson(Object data) {
            return json(data);
        }

        /**
         * Returns a {@link RenderJsonMap} result with any object. This method will
         * generate a JSON object out from the {@link ActionContext#renderArgs}.
         * The response is always in JSON format and ignores the HTTP `Accept`
         * header setting
         *
         * @param data
         *         the varargs of Object to be put into the JSON map
         * @return the result
         */
        public static RenderJsonMap jsonMap(Object... data) {
            return RenderJsonMap.get();
        }

        /**
         * Alias of {@link #jsonMap(Object...)}
         *
         * @param data
         *         the data to be put into the JSON map
         * @return the result
         */
        public static RenderJsonMap renderJsonMap(Object... data) {
            return jsonMap(data);
        }

        public static RenderJwt renderJwt() {
            return RenderJwt.get();
        }

        public static RenderJwt jwt() {
            return RenderJwt.get();
        }

        /**
         * Returns a {@link RenderXML} result with specified message template
         * and args. The final message is rendered with the template and arguments using
         * {@link String#format(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderXML xml(String msg, Object... args) {
            return RenderXML.of(successStatus(), msg, args);
        }

        /**
         * Alias of {@link #xml(String, Object...)}
         *
         * @param msg
         *         the message format template
         * @param args
         *         the message format arguments
         * @return the result
         */
        public static RenderXML renderXml(String msg, Object... args) {
            return xml(msg, args);
        }

        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "inline" content disposition
         *
         * @param sobj
         *         the {@link ISObject} instance
         * @return the result
         */
        public static RenderBinary binary(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), true);
        }

        /**
         * Alias of {@link #binary(ISObject)}
         *
         * @param sobj
         *         the {@link ISObject} instance
         * @return the result
         */
        public static RenderBinary renderBinary(ISObject sobj) {
            return binary(sobj);
        }

        /**
         * Returns a {@link RenderBinary} result with an {@link ISObject} instance. The result will render
         * the binary using "attachment" content disposition
         *
         * @param sobj
         *         the {@link ISObject} instance
         */
        public static RenderBinary download(ISObject sobj) {
            return new RenderBinary(sobj.asInputStream(), sobj.getAttribute(ISObject.ATTR_FILE_NAME), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), false);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "inline" content disposition.
         *
         * @param file
         *         the file to be rendered
         * @return a result
         */
        public static RenderBinary binary(File file) {
            return new RenderBinary(file);
        }

        /**
         * Alias of {@link #binary(File)}
         *
         * @param file
         *         the file to be rendered
         * @return a result
         */
        public static RenderBinary renderBinary(File file) {
            return binary(file);
        }

        /**
         * Returns a {@link RenderBinary} result with a delayed output stream writer.
         * The result will render the binary using "inline" content disposition.
         *
         * @param outputStreamWriter
         *         the delayed writer
         * @return the result
         */
        public static RenderBinary binary($.Visitor<Output> outputStreamWriter) {
            return new RenderBinary(outputStreamWriter);
        }

        /**
         * Alias of {@link #binary(org.osgl.Lang.Visitor)}
         *
         * @param outputStreamWriter
         *         the delayed writer
         * @return the result
         */
        public static RenderBinary renderBinary($.Visitor<Output> outputStreamWriter) {
            return binary(outputStreamWriter);
        }

        /**
         * Returns a {@link RenderBinary} result with byte array.
         * The result will render the binary using "inline" content disposition.
         *
         * @param blob
         *      the body of byte array
         * @return the result
         */
        public static RenderBinary binary(byte[] blob) {
            return new RenderBinary(blob);
        }

        /**
         * Alias of {@link #binary(byte[])}
         */
        public static RenderBinary renderBinary(byte[] blob) {
            return binary(blob);
        }

        /**
         * Returns a {@link RenderBinary} result with byte array and an `attachmentName`.
         *
         * The result will render the binary using "inline" content disposition
         * if the `attachmentName` is empty string or "attachment" if the `attachementName`
         * is non empty string.
         *
         * @param blob
         *      the body of byte array
         * @return the result
         */
        public static RenderBinary binary(byte[] blob, String attachmentName) {
            return new RenderBinary(blob, attachmentName);
        }

        /**
         * Alias of {@link #binary(byte[], String)}
         */
        public static RenderBinary renderBinary(byte[] blob, String attachmentName) {
            return binary(blob, attachmentName);
        }

        /**
         * Returns a {@link RenderBinary} result with a URL. The result will render
         * the binary using "attachment" content disposition.
         *
         * @param url
         *         the URL to be rendered
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(URL url) {
            return new RenderBinary(IO.is(url), ActionContext.current().attachmentName(), false);
        }

        /**
         * Returns a {@link RenderBinary} result with a URL. The result will render
         * the binary using "attachment" content disposition.
         *
         * @param url
         *         the URL to be rendered
         * @param attachmentName
         *         the attachment name
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(URL url, String attachmentName) {
            return new RenderBinary(IO.is(url), attachmentName, false);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will render
         * the binary using "attachment" content disposition.
         *
         * @param file
         *         the file to be rendered
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(File file) {
            return new RenderBinary(file, file.getName(), false);
        }

        /**
         * Returns a {@link RenderBinary} result with a file. The result will
         * render the binary using "attachment" content disposition, with
         * the `attachmentName` as the name of the download attachment.
         *
         * @param file
         *         the file to be downloaded
         * @param attachmentName
         *         the attachment file name.
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(File file, String attachmentName) {
            return new RenderBinary(file, attachmentName, false);
        }

        /**
         * Returns a {@link RenderBinary} result with an `InputStream`. The result will
         * render the binary using "attachment" content disposition, with
         * {@link ActionContext#attachmentName()} as the name of the download attachment.
         *
         * @param inputStream
         *         the input stream from which byte content will be written to the
         *         attachment.
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(InputStream inputStream) {
            return new RenderBinary(inputStream, ActionContext.current().attachmentName(), false);
        }

        /**
         * Returns a {@link RenderBinary} result with an `InputStream`. The result will
         * render the binary using "attachment" content disposition, with
         * the `attachmentName` as the name of the download attachment.
         *
         * @param inputStream
         *         the input stream from which byte content will be written to the
         *         attachment.
         * @param attachmentName
         *         the attachment file name.
         * @return a `RenderBinary` result as described above
         */
        public static RenderBinary download(InputStream inputStream, String attachmentName) {
            return new RenderBinary(inputStream, attachmentName, false);
        }

        /**
         * Render barcode for given content
         *
         * @param content
         *         the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult barcode(String content) {
            return ZXingResult.barcode(content);
        }

        /**
         * Alias of {@link #barcode(String)}
         *
         * @param content
         *         the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult renderBarcode(String content) {
            return barcode(content);
        }

        /**
         * Render QRCode for given content
         *
         * @param content
         *         the content to generate the qrcode
         * @return the qrcode as a binary result
         */
        public static ZXingResult qrcode(String content) {
            return ZXingResult.qrcode(content);
        }

        /**
         * Alias of {@link #qrcode(String)}
         *
         * @param content
         *         the content to generate the barcode
         * @return the barcode as a binary result
         */
        public static ZXingResult renderQrcode(String content) {
            return qrcode(content);
        }

        /**
         * Returns a {@link RenderTemplate} result with a render arguments map.
         * Note the template path should be set via {@link ActionContext#templatePath(String)}
         * method
         *
         * @param args
         *         the template arguments
         * @return a result to render template
         */
        public static RenderTemplate template(Map<String, Object> args) {
            return RenderTemplate.of(args);
        }

        /**
         * Alias of {@link #template(Map)}
         *
         * @param args
         *         the template arguments
         * @return a result to render template
         */
        public static RenderTemplate renderTemplate(Map<String, Object> args) {
            return template(args);
        }

        /**
         * This method is deprecated, please use {@link #template(Object...)} instead
         *
         * @param args
         *         template argument list
         */
        public static RenderTemplate renderTemplate(Object... args) {
            return RenderTemplate.get();
        }

        /**
         * Kind of like {@link #render(Object...)}, the only differences is this method force to render a template
         * without regarding to the request format
         *
         * @param args
         *         template argument list
         */
        public static RenderTemplate template(Object... args) {
            return RenderTemplate.get(ActionContext.current().successStatus());
        }

        /**
         * The caller to this magic {@code render} method is subject to byte code enhancement. All
         * parameter passed into this method will be put into the application context via
         * {@link ActionContext#renderArg(String, Object)} using the variable name found in the
         * local variable table. If the first argument is of type String and there is no variable name
         * associated with that variable then it will be treated as template path and get set to the
         * context via {@link ActionContext#templatePath(String)} method.
         * <p>This method returns different render results depends on the request format</p>
         * <table>
         * <tr>
         * <th>Format</th>
         * <th>Result type</th>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#json}</td>
         * <td>A JSON string that map the arguments to their own local variable names</td>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#html} or any other text formats</td>
         * <td>{@link RenderTemplate}</td>
         * </tr>
         * <tr>
         * <td>{@link org.osgl.http.H.Format#pdf} or any other binary format</td>
         * <td>If first argument is of type File or InputStream, then outbound the
         * content as a binary stream, otherwise throw out {@link org.osgl.exception.UnsupportedException}</td>
         * </tr>
         * </table>
         *
         * @param args
         *         any argument that can be put into the returned JSON/XML data or as template arguments
         */
        public static RenderAny render(Object... args) {
            return RenderAny.get();
        }

        public static Result inferResult(Result r, ActionContext actionContext) {
            return r;
        }

        public static Result inferPrimitiveResult(Object v, ActionContext actionContext, boolean requireJSON, boolean requireXML, boolean isArray, boolean isDateTime) {
            H.Status status = actionContext.successStatus();
            if (requireJSON) {
                if (isArray) {
                    return RenderJSON.of(status, $.toString2(v));
                }
                return RenderJSON.of(status, v instanceof String ? v : C.Map("result", v));
            } else if (requireXML) {
                return RenderXML.of(status, S.concat("<result>", S.string(v), "</result>"));
            } else if (v instanceof byte[]) {
                H.Format fmt = actionContext.accept();
                if (H.Format.UNKNOWN == fmt) {
                    actionContext.resp().contentType("application/octet-stream");
                }
                return new RenderBinary((byte[]) v);
            } else {
                H.Format fmt = actionContext.accept();
                String s = $$.toString(v, isDateTime, isArray);
                if (HTML == fmt) {
                    return RenderHtml.of(status, s);
                }
                if (fmt.isText()) {
                    return RenderText.of(status, fmt, s, status.toString());
                }
                DirectRender dr = Act.viewManager().loadDirectRender(actionContext);
                if (null == dr) {
                    throw E.unexpected("Cannot apply text result to format: %s", fmt);
                }
                return new DirectRenderResult(dr, v);
            }
        }

        public static Result inferResult(Map<String, Object> map, ActionContext actionContext) {
            if (actionContext.acceptJson()) {
                return RenderJSON.of(actionContext.successStatus(), map);
            }
            return RenderTemplate.of(actionContext.successStatus(), map);
        }

        /**
         * @param array
         * @param actionContext
         * @return
         */
        public static Result inferResult(Object[] array, ActionContext actionContext) {
            if (actionContext.acceptJson()) {
                return RenderJSON.of(actionContext.successStatus(), array);
            }
            throw E.tbd("render template with render args in array");
        }

        /**
         * Infer {@link Result} from an {@link InputStream}. If the current context is in
         * {@code JSON} format then it will render a {@link RenderJSON JSON} result from the content of the
         * input stream. Otherwise, it will render a {@link RenderBinary binary} result from the inputstream
         *
         * @param is
         *         the inputstream
         * @param actionContext
         * @return a Result inferred from the inputstream specified
         */
        public static Result inferResult(InputStream is, ActionContext actionContext) {
            if (actionContext.acceptJson()) {
                return RenderJSON.of(actionContext.successStatus(), IO.readContentAsString(is));
            } else {
                // name must be a blank string, `null` will trigger
                // NPE in SObject lib
                return new RenderBinary(is, "", true).status(actionContext.successStatus());
            }
        }

        /**
         * Infer {@link Result} from an {@link File}. If the current context is in
         * {@code JSON} format then it will render a {@link RenderJSON JSON} result from the content of the
         * file. Otherwise, it will render a {@link RenderBinary binary} result from the file specified
         *
         * @param file
         *         the file
         * @param actionContext
         * @return a Result inferred from the file specified
         */
        public static Result inferResult(File file, ActionContext actionContext) {
            if (null == file || !file.exists()) {
                return notFound();
            }
            if (!file.canRead()) {
                return forbidden();
            }
            if (actionContext.acceptJson()) {
                return RenderJSON.of(actionContext.successStatus(), IO.readContentAsString(file));
            } else {
                return new RenderBinary(file).status(actionContext.successStatus());
            }
        }

        public static Result inferResult(ISObject sobj, ActionContext context) {
            if (context.acceptJson()) {
                return RenderJSON.of(context.successStatus(), sobj.asString());
            } else {
                return binary(sobj).status(context.successStatus());
            }
        }

        /**
         * Infer a {@link Result} from a {@link Object object} value v:
         * <ul>
         * <li>If v is {@code null} then null returned</li>
         * <li>If v is instance of {@code Result} then it is returned directly</li>
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code InputStream} then {@link #inferResult(InputStream, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code File} then {@link #inferResult(File, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is instance of {@code Map} then {@link #inferResult(Map, ActionContext)} is used
         * to infer the {@code Result}</li>
         * <li>If v is an array of {@code Object} then {@link #inferResult(Object[], ActionContext)} is used
         * to infer the {@code Result}</li>
         * </ul>
         *
         * @param meta
         *         the HandlerMethodMetaInfo
         * @param v
         *         the value to be rendered
         * @param context
         *         the action context
         * @param hasTemplate
         *         a boolean flag indicate if the current handler method has corresponding template
         * @return the rendered result
         */
        public static Result inferResult(HandlerMethodMetaInfo meta, Object v, ActionContext context, boolean hasTemplate) {
            if (v instanceof Result) {
                return (Result) v;
            }
            final H.Request req = context.req();
            final H.Status status = context.successStatus();
            if (Act.isProd() && v instanceof Versioned && req.method().safe()) {
                processEtag(meta, v, context, req);
            }
            if (hasTemplate) {
                if (v instanceof Map) {
                    return inferToTemplate(((Map) v), context);
                }
                return inferToTemplate(v, context);
            }

            H.Format accept = context.accept();
            boolean requireJSON = (accept == H.Format.JSON) || (accept == H.Format.UNKNOWN);
            boolean requireXML = !requireJSON && accept == H.Format.XML;

            if (null == v) {
                // the following code breaks before handler without returning result
                //return requireJSON ? RenderJSON.of("{}") : requireXML ? RenderXML.of("<result></result>") : null;
                return null;
            }
            Class vCls = v.getClass();
            boolean isDateTimeType = $$.isDateTimeType(vCls);
            if ($.isSimpleType(vCls) || isDateTimeType) {
                boolean isArray = vCls.isArray();
                return inferPrimitiveResult(v, context, requireJSON, requireXML, isArray, isDateTimeType);
            } else if (v instanceof InputStream) {
                return inferResult((InputStream) v, context);
            } else if (v instanceof File) {
                return inferResult((File) v, context);
            } else if (v instanceof ISObject) {
                return inferResult((ISObject) v, context);
            } else if (v instanceof Map) {
                return RenderJSON.of(status, v);
            } else {
                if (requireJSON || H.Format.UNKNOWN == context.req().accept()) {
                    boolean isIterable = v instanceof Iterable;
                    if (isIterable) {
                        v = new FastJsonIterable((Iterable) v);
                    }
                    // no need to check string case as it is already checked above
                    if (v instanceof $.Visitor) {
                        return RenderJSON.of(($.Visitor) v);
                    } else if (v instanceof $.Func0) {
                        return RenderJSON.of(($.Func0) v);
                    }
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    boolean possibleLargeResponse = context.isLargeResponse();
                    JsonWriter jsonWriter = new JsonWriter(v, propertySpec, false, context);
                    return possibleLargeResponse ? RenderJSON.of(jsonWriter) : RenderJSON.of(jsonWriter.asContentProducer());
                } else if (context.acceptXML()) {
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    return new FilteredRenderXML(status, v, propertySpec, context);
                } else if (context.accept() == H.Format.CSV) {
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    return RenderCSV.of(status, v, propertySpec, context);
                } else {
                    boolean isArray = vCls.isArray();
                    PropertySpec.MetaInfo propertySpec = PropertySpec.MetaInfo.withCurrent(meta, context);
                    context.propertySpec(propertySpec);
                    return inferPrimitiveResult(v, context, false, requireXML, isArray, isDateTimeType);
                }
            }
        }

        private static void processEtag(HandlerMethodMetaInfo meta, Object v, ActionContext context, H.Request req) {
            if (!(v instanceof Versioned)) {
                return;
            }
            String version = ((Versioned) v)._version();
            String etagVersion = etag(meta, version);
            if (req.etagMatches(etagVersion)) {
                throw NotModified.get();
            } else {
                context.resp().etag(etagVersion);
            }
        }

        private static String etag(HandlerMethodMetaInfo meta, String version) {
            return S.newBuffer(version).append(meta.hashCode()).toString();
        }

        private static Result inferToTemplate(Object v, ActionContext actionContext) {
            actionContext.renderArg("result", v);
            return RenderTemplate.get();
        }

        private static Result inferToTemplate(Map map, ActionContext actionContext) {
            return RenderTemplate.of(map);
        }

        private static H.Status successStatus() {
            return ActionContext.current().successStatus();
        }
    }

    /**
     * Controller class extends this class automatically get `ActionContext` injected
     * as a field
     */
    class Base extends Util {
        @Inject
        protected ActionContext context;
    }

}
