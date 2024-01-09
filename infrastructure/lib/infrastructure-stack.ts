import { Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subs from 'aws-cdk-lib/aws-sns-subscriptions';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import { Construct } from 'constructs';
import { Effect, ManagedPolicy, PolicyDocument, PolicyStatement } from 'aws-cdk-lib/aws-iam';

// ref: setup pools https://stackoverflow.com/questions/55784746/how-to-create-cognito-identitypool-with-cognito-userpool-as-one-of-the-authentic

export class InfrastructureStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const userPool = new cognito.UserPool(this, "test42", {
      removalPolicy: RemovalPolicy.DESTROY,
    })

    const userPoolClient = new cognito.UserPoolClient(
      this,
      'test42client',
      {
        userPool,
        authFlows: {
          adminUserPassword: true,
        },
        supportedIdentityProviders: [
          cognito.UserPoolClientIdentityProvider.COGNITO,
        ],
      },
    );

    const identityPool = new cognito.CfnIdentityPool(this, 'test42identity', {
      allowUnauthenticatedIdentities: false,
      cognitoIdentityProviders: [{
        clientId: userPoolClient.userPoolClientId,
        providerName: userPool.userPoolProviderName
      }],
    });

    const authenticatedRole = new iam.Role(this, 'test42AuthRole', {
      assumedBy: new iam.FederatedPrincipal('cognito-identity.amazonaws.com', {
          "StringEquals": { "cognito-identity.amazonaws.com:aud": identityPool.ref },
          "ForAnyValue:StringLike": { "cognito-identity.amazonaws.com:amr": "authenticated" },
      }, "sts:AssumeRoleWithWebIdentity"),
      inlinePolicies: {
        cognitoBasePolicy: new PolicyDocument({
          statements: [
            new PolicyStatement({
              effect: Effect.ALLOW,
              actions: [
                  "mobileanalytics:PutEvents",
                  "cognito-sync:*",
                  "cognito-identity:*"
              ],
              resources: ["*"],
          })],
        })},
      managedPolicies: [ ManagedPolicy.fromAwsManagedPolicyName('AWSIoTFullAccess') ],
    });

    const defaultPolicy = new cognito.CfnIdentityPoolRoleAttachment(this, 'DefaultValid', {
      identityPoolId: identityPool.ref,
      roles: {
          // 'unauthenticated': unauthenticatedRole.roleArn,
          'authenticated': authenticatedRole.roleArn
      }
  });
  }
}
