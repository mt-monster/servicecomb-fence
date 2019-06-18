/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.authentication.edge;

import org.apache.servicecomb.authentication.token.JWTToken;
import org.apache.servicecomb.authentication.token.JWTTokenStore;
import org.apache.servicecomb.authentication.token.OpenIDToken;
import org.apache.servicecomb.authentication.token.OpenIDTokenStore;
import org.apache.servicecomb.authentication.util.Constants;
import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;

public class AuthHandler implements Handler {
  @Override
  public void handle(Invocation invocation, AsyncResponse asyncResponse) throws Exception {
    String token = invocation.getContext(Constants.CONTEXT_HEADER_AUTHORIZATION);
    String tokenType = invocation.getContext(Constants.CONTEXT_HEADER_AUTHORIZATION_TYPE);
    if (token == null) {
      asyncResponse.consumerFail(new InvocationException(403, "forbidden", "not authenticated"));
      return;
    }

    if (Constants.CONTEXT_HEADER_AUTHORIZATION_TYPE_ID_TOKEN.equals(tokenType)) {
      JWTTokenStore jwtTokenStore = BeanUtils.getBean(Constants.BEAN_AUTH_ID_TOKEN_STORE);
      JWTToken jwtToken = jwtTokenStore.createTokenByValue(token);
      if (jwtToken == null || jwtToken.isExpired()) {
        asyncResponse.consumerFail(new InvocationException(403, "forbidden", "not authenticated"));
        return;
      }

      // send id_token to services to apply state less validation
      invocation.addContext(Constants.CONTEXT_HEADER_AUTHORIZATION, jwtToken.getValue());
      invocation.next(asyncResponse);
    } else if (Constants.CONTEXT_HEADER_AUTHORIZATION_TYPE_SESSION_TOKEN.equals(tokenType)) {
      OpenIDTokenStore openIDTokenStore = BeanUtils.getBean(Constants.BEAN_AUTH_OPEN_ID_TOKEN_STORE);


      OpenIDToken tokenResonse = openIDTokenStore.readTokenByValue(token);
      if (tokenResonse == null || tokenResonse.isExpired()) {
        asyncResponse.consumerFail(new InvocationException(403, "forbidden", "not authenticated"));
        return;
      }

      // send id_token to services to apply state less validation
      invocation.addContext(Constants.CONTEXT_HEADER_AUTHORIZATION, tokenResonse.getIdToken().getValue());
      invocation.next(asyncResponse);
    } else {
      asyncResponse.consumerFail(new InvocationException(403, "forbidden", "not authenticated"));
      return;
    }
  }
}