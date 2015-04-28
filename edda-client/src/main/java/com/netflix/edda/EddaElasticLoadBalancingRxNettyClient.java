/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.edda;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingRxNetty;
import com.amazonaws.services.elasticloadbalancing.model.*;

import com.netflix.edda.mapper.InstanceStateView;
import com.netflix.edda.mapper.LoadBalancerAttributesView;

import com.amazonaws.services.ServiceResult;
import com.amazonaws.services.NamedServiceResult;
import com.amazonaws.services.PaginatedServiceResult;

import rx.Observable;

public class EddaElasticLoadBalancingRxNettyClient extends EddaAwsRxNettyClient {
  public EddaElasticLoadBalancingRxNettyClient(AwsConfiguration config, String vip, String region) {
    super(config, vip, region);
  }

  public AmazonElasticLoadBalancingRxNetty readOnly() {
    return readOnly(AmazonElasticLoadBalancingRxNetty.class);
  }

  public AmazonElasticLoadBalancingRxNetty wrapAwsClient(AmazonElasticLoadBalancingRxNetty delegate) {
    return wrapAwsClient(AmazonElasticLoadBalancingRxNetty.class, delegate);
  }

  public Observable<NamedServiceResult<DescribeInstanceHealthResult>> describeInstanceHealth() {
    return Observable.defer(() -> {
      TypeReference<List<InstanceStateView>> ref = new TypeReference<List<InstanceStateView>>() {};
      String url = config.url() + "/api/v2/view/loadBalancerInstances;_expand";
      return doGet(url).flatMap(sr -> {
        try {
          return Observable.from(parse(ref, sr.result)).map(view -> {
            return new NamedServiceResult<DescribeInstanceHealthResult>(
              sr.startTime,
              view.getName(),
              new DescribeInstanceHealthResult().withInstanceStates(view.getInstances())
            );
          });
        }
        catch (IOException e) {
          throw new AmazonClientException("Faled to parse " + url, e);
        }
      });
    });
  }

  public Observable<ServiceResult<DescribeInstanceHealthResult>> describeInstanceHealth(
    final DescribeInstanceHealthRequest request
  ) {
    return Observable.defer(() -> {
      validateNotEmpty("LoadBalancerName", request.getLoadBalancerName());

      TypeReference<InstanceStateView> ref = new TypeReference<InstanceStateView>() {};
      String loadBalancerName = request.getLoadBalancerName();
    
      String url = config.url() + "/api/v2/view/loadBalancerInstances/"+loadBalancerName+";_expand";
      return doGet(url).map(sr -> {
        try {
          InstanceStateView view = parse(ref, sr.result);
          List<InstanceState> instanceStates = view.getInstances();

          List<Instance> instances = request.getInstances();
          List<String> ids = new ArrayList<String>();
          if (instances != null) {
            for (Instance i : instances)
              ids.add(i.getInstanceId());
          }
          if (shouldFilter(ids)) {
            List<InstanceState> iss = new ArrayList<InstanceState>();
            for (InstanceState is : instanceStates) {
              if (matches(ids, is.getInstanceId()))
                iss.add(is);
            }
            instanceStates = iss;
          }

          return new ServiceResult<DescribeInstanceHealthResult>(
            sr.startTime,
            new DescribeInstanceHealthResult().withInstanceStates(view.getInstances())
          );
        }
        catch (IOException e) {
          throw new AmazonClientException("Faled to parse " + url, e);
        }
      });
    });
  }

  public Observable<PaginatedServiceResult<DescribeLoadBalancersResult>> describeLoadBalancers() {
    return describeLoadBalancers(new DescribeLoadBalancersRequest());
  }

  public Observable<PaginatedServiceResult<DescribeLoadBalancersResult>> describeLoadBalancers(
    final DescribeLoadBalancersRequest request
  ) {
    return Observable.defer(() -> {
      TypeReference<List<LoadBalancerDescription>> ref = new TypeReference<List<LoadBalancerDescription>>() {};
      String url = config.url() + "/api/v2/aws/loadBalancers;_expand";
      return doGet(url).map(sr -> {
        try {
          List<LoadBalancerDescription> loadBalancerDescriptions = parse(ref, sr.result);

          List<String> names = request.getLoadBalancerNames();
          if (shouldFilter(names)) {
            List<LoadBalancerDescription> lbs = new ArrayList<LoadBalancerDescription>();
            for (LoadBalancerDescription lb : loadBalancerDescriptions) {
              if (matches(names, lb.getLoadBalancerName()))
                lbs.add(lb);
            }
            loadBalancerDescriptions = lbs;
          }

          return new PaginatedServiceResult<DescribeLoadBalancersResult>(
            sr.startTime,
            null,
            new DescribeLoadBalancersResult().withLoadBalancerDescriptions(loadBalancerDescriptions)
          );
        }
        catch (IOException e) {
          throw new AmazonClientException("Faled to parse " + url, e);
        }
      });
    });
  }

  public Observable<NamedServiceResult<DescribeLoadBalancerAttributesResult>> describeLoadBalancerAttributes() {
    return Observable.defer(() -> {
      TypeReference<List<LoadBalancerAttributesView>> ref = new TypeReference<List<LoadBalancerAttributesView>>() {};
      String url = config.url() + "/api/v2/view/loadBalancerAttributes;_expand";
      return doGet(url).flatMap(sr -> {
        try {
          return Observable.from(parse(ref, sr.result)).map(view -> {
            return new NamedServiceResult<DescribeLoadBalancerAttributesResult>(
              sr.startTime,
              view.getName(),
              new DescribeLoadBalancerAttributesResult().withLoadBalancerAttributes(view.getAttributes())
            );
          });
        }
        catch (IOException e) {
          throw new AmazonClientException("Faled to parse " + url, e);
        }
      });
    });
  }

  public Observable<ServiceResult<DescribeLoadBalancerAttributesResult>> describeLoadBalancerAttributes(
    final DescribeLoadBalancerAttributesRequest request
  ) {
    return Observable.defer(() -> {
      validateNotEmpty("LoadBalancerName", request.getLoadBalancerName());
      TypeReference<LoadBalancerAttributesView> ref = new TypeReference<LoadBalancerAttributesView>() {};
      String loadBalancerName = request.getLoadBalancerName();
      String url = config.url() + "/api/v2/view/loadBalancerAttributes/"+loadBalancerName+";_expand";
      return doGet(url).map(sr -> {
        try {
          LoadBalancerAttributesView view = parse(ref, sr.result);
          return new ServiceResult<DescribeLoadBalancerAttributesResult>(
            sr.startTime,
            new DescribeLoadBalancerAttributesResult().withLoadBalancerAttributes(view.getAttributes())
          );
        }
        catch (IOException e) {
          throw new AmazonClientException("Faled to parse " + url, e);
        }
      });
    });
  }
}