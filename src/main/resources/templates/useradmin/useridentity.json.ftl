<#ftl encoding="utf-8">
{
 "personRef":"${identity.personRef!?js_string}",
 "uid":"${identity.uid?js_string}",
 "username":"${identity.username?js_string}",
 "firstName":"${identity.firstName?js_string}",
 "lastName":"${identity.lastName?js_string}",
 "email":"${identity.email!?js_string}",
 "cellPhone":"${identity.cellPhone!?js_string}",
 "uri":"${userbaseurl+"useradmin/users/"+identity.uid+"/"?js_string}"
}