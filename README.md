# CxEngage Javascript SDK

## Development

To run the project:

`boot dev`

Then open your browser to `http://localhost:3449/`.

To connect via repl once `dev` is running:

`boot repl` followed by `(start-repl)`

To run tests once:

`boot test-once`

To run tests in the background and have them re-run every time you make a change:

`boot test`

To make a prod release (you shouldn't ever really have to do this; jenkins does it for us)

`boot make-prod-release`

To make a prod alike release for development purposes (includes source maps, which make-prod-release doesn't, to help with development)

`boot make-dev-release`

To run the documentation generator:

`boot docs`

# Documentation

The `def-sdk-fn` macro we use for 90% of our functions now accepts a docstring as
it's first parameter, which is then picked up by our documentation generator (Powered by Codox).
Below I will demonstrate what will be expected as documentation for any new functions
added to the SDK. Our docs generator supports [Markdown](https://guides.github.com/features/mastering-markdown/)
so please ensure you format your documentation properly. I'll be using the popIdentityPage
function from the Authentication Module as an example. All functions will be picked up
by the docs generator, so to ensure helper functions and non-public-facing functions
do not show up, be sure to define them as private with `defn-`.

## Description

The first section in your docstring should be developer-perspective explanation of
what the function's purpose is and in what scenarios it should be invoked. Do not
be afraid of being verbose, provide examples if necessary.

```
The getAuthInfo function is used to retrieve a user's Single Sign On details,
and when used in conjunction with the popIdentityPage function - will open a
window for a user to sign into their third party SAML provider. There are
multiple ways to call this function to get a particular identity provider for
a user.
```

## Code Examples

The second section is to demonstrate how to invoke the function in Javascript,
as well as explaining what parameters it needs. The following examples are from
the popIdentityPage, as state above, and the three different methods of invoking
it, and what each difference means.

```
The first way is to simply use their email - which will grab their
default tenant's 'client' and 'domain' fields.

CxEngage.authentication.getAuthInfo({
    username: '{{string}}'
})

The second way to use this function is to specify a tenant ID - which will be
used to retrieve that tenant's default identity provider information in order
to open the third party sign on window.

CxEngage.authentication.getAuthInfo({
    tenantId: '{{uuid}}'
})

The third way is to specify an identity provider ID in addition to a tenant ID
in order to retrieve the information if it is not the default identity provider
on the tenant.

CxEngage.authentication.getAuthInfo({
    tenantId: '{{uuid}}',
    idpId: '{{uuid}}'
})
```

## Errors Paths

Every function should have errors associated with each of the potential "sad paths"
within it. These errors should be noted here along with a link to the errors page,
achoring to the specific one they are looking for. We should not document anything
specific about the error, as it will be covered in the errors namespace.

```
Possible Errors:

- [Authentication: 3005](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-auth-info-err)
```

## Error Descriptions

Any new error added to the errors namespace should also be accompanied by four
things:

* The Error Code assigned to it
* The short-form message description provided to the frontend
* The long-form description of potential causes for this particular error
* The internal suggested solutions to be utilized by Serenova's Support team

```
**Error Code:** 3005
Message: Failed to retrieve SSO authentication information.

This error can be thrown if we are unable to find any SSO information
associated with their email address or tenant ID / identity provider ID
combination.

**Solution:** Ensure that the email or tenant being used is properly associated
with a valid identity provider, and that the SAML provider is configured
correctly.
```
