https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-active-directory
https://medium.com/xebia-engineering/authentication-and-authorization-using-azure-active-directory-266980586ab8

Step 1 : Creating Our Web App
Browse to https://start.spring.io/.
Specify that you want to generate a Maven project with Java, enter the Group and Artifact names for your application.
Add Dependencies for Spring Web, Azure Active Directory, and Spring Security, Spring Data JPA, OAuth2Client, H2 Database
At the bottom of the page and click the Generate button.
Step 2: Configuring Active Directory
Creating Tenant
Sign in to the Azure portal
Now you have to create a tenant. Tenants are instances that provide access to an environment. More information can be found here.
Select “All resources”, and look for “Azure Active Directory” and click “Create a tenant”
Select “Azure Active Directory” option
Fill in your organization’s name, domain, and country, and you’re done!
You can now switch to your Active Directory tenant by clicking on the Switch Directory on the top menu.
Image for post
Registering the application :
In the left-hand navigation pane, select the Azure Active Directory service, and then select “App registrations” and create a new registration
In the Name section, enter a meaningful application name that will be displayed to users of the app
In the Supported account types section, select Accounts in any organizational directory.
Add http://localhost:8080 as the Reply URL under Redirect URI.
Select Register to create the application.
In the “Overview”, note the “Application (client) ID”, this is what will be used in Spring Security as “client-id”, as well as the “Directory (tenant) ID”, which will be Spring Security’s “tenant-id”.These will be configured in the application.properties of this project
Select “Authentication” in the left navigation pane, and in the Web “Platform configuration”, check both options under “Implicit grant” (“Access tokens” and “ID tokens”)
Select “Certificates & secrets” in the left navigation pane, go to that page and in the Client secrets section, choose New client secret:
Type a key description (of instance app secret),
Select a key duration of either In 1 year, In 2 years, or Never Expires.
When you press the Add button, the key value will be displayed, copy, and save the value in a safe location.
You’ll need this key later to configure the project. This key value will not be displayed again, nor retrievable by any other means, so record it as soon as it is visible from the Azure portal.
Select “API permissions” in the left tab
Click on “Add a permission” button and then ensure that the Microsoft APIs tab is selected
In the Commonly used Microsoft APIs section, click on Microsoft Graph
In the Delegated permissions section, ensure that the right permissions are checked: Directory.AccessAsUser.All and “User. Read” permissions
Click on the “Grant admin consent” button at the bottom of the page
Select the Add permissions button
Image for post
Step 3: Configuring Users & Groups for your application
Still, in your Active Directory tenant, select “Groups” and create a new group, for example, “group1”.
Now select “Users”, create a new user, and give that user the “group1” group that we just created.
Image for post
Step 4: Configuring your Spring boot Application
Extract the files from the project archive you created and downloaded it in the earlier steps into a directory.
Navigate to the src/main/resources folder in your project and open the application.properties files and configure the following properties which were recorded in the earlier steps.
# Specifies your Active Directory ID:
azure.activedirectory.tenant-id=4e5f4cb7–9489–4187–8217–515252fcb09e
# Specifies your App Registration’s Application ID:
spring.security.oauth2.client.registration.azure.client-id=0256cf7c-0e57–4305-a278-d9b2599798c9
# Specifies your App Registration’s secret key:
spring.security.oauth2.client.registration.azure.client-secret=80sZw5TbL?_1pAg-:oLig35TGz94RI=q
# Specifies the list of Active Directory groups to use for authorization:
azure.activedirectory.active-directory-groups=Academy
3. To define customized roles for your application you need to extend OAuth2UserService and override the loadUser method to map the authenticated user with their respective roles.
For instance, In the sample provided below, I have created the APIs for 2 specific roles i.e student and coach. Based on which the authorization is provided in the controller layer.
@Component
public class CustomADOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
 public static final String ROLE_COACH = "ROLE_coach";
 public static final String ROLE_STUDENT = "ROLE_student";
 @Autowired
 StudentRepository studentRepository;
 @Autowired
 CoachRepository coachRepository;
AADOAuth2UserService aadoAuth2UserService;
public CustomADOAuth2UserService(AADAuthenticationProperties aadAuthProps,
 ServiceEndpointsProperties serviceEndpointsProps) {
 aadoAuth2UserService = new AADOAuth2UserService(aadAuthProps, serviceEndpointsProps);
 }
@Override
 public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
 OidcUser user = aadoAuth2UserService.loadUser(userRequest);
 Set<GrantedAuthority> mappedAuthorities = new HashSet<>(user.getAuthorities());
 String userName = (String) user.getIdToken().getClaims().get("unique_name");
 Optional<Student> optionalStudent = studentRepository.findByUserId(userName);
 optionalStudent.ifPresent(vendorUser ->
 mappedAuthorities.add(new SimpleGrantedAuthority(ROLE_STUDENT)));
 if (!optionalStudent.isPresent()) {
 Optional<Coach> optionalCoach = coachRepository.findByUsername(userName);
 optionalCoach.ifPresent(companyUser -> mappedAuthorities
 .add(new SimpleGrantedAuthority(ROLE_COACH)));
}
 return new DefaultOidcUser(mappedAuthorities, user.getIdToken(),
 this.getUserNameAttrName(userRequest));
 }
private String getUserNameAttrName(OAuth2UserRequest userRequest) {
 String userNameAttrName = userRequest.getClientRegistration()
 .getProviderDetails()
 .getUserInfoEndpoint()
 .getUserNameAttributeName();
 if (StringUtils.isEmpty(userNameAttrName)) {
 userNameAttrName = "name";
 }
 return userNameAttrName;
 }
}
4. The code that hooks up Azure AD authentication is in the SecurityConfig class. This configuration will require that each request is secured, and will, therefore, redirect any user to Active Directory when he tries to connect.
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
@Autowired
 private AADAuthenticationProperties aadAuthProps;
 @Autowired
 private ServiceEndpointsProperties serviceEndpointsProps;
@Override
 protected void configure(HttpSecurity http) throws Exception {
 http
 .authorizeRequests()
 .anyRequest().authenticated()
 .and()
 .oauth2Login()
 .userInfoEndpoint()
 .oidcUserService(customADOAuth2UserService());
 }
@Bean
 protected OAuth2UserService<OidcUserRequest, OidcUser> customADOAuth2UserService() {
 return new CustomADOAuth2UserService(aadAuthProps, serviceEndpointsProps);
 }
}
5. Now let’s finally create the APIs to know if everything is correct.
@RestController
@RequestMapping("/api")
public class CoachController {
@GetMapping("/coaches")
@PreAuthorize("hasRole('coach')")
 public String helloCoach(Authentication authentication) {
 DefaultOidcUser principal = (DefaultOidcUser) authentication.getPrincipal();
 String username = principal.getName();
 return "Welcome Coach! " + username;
 }
}
@RestController
@RequestMapping("/api")
public class StudentController {
@GetMapping("/students")
@PreAuthorize("hasRole('student')")
 public String helloStudent(Authentication authentication) {
 DefaultOidcUser principal = (DefaultOidcUser) authentication.getPrincipal();
 String username = principal.getName();
 return "Welcome Student! " + username;
 }
}
6. Accessing http://localhost:8080/api/coaches and http://localhost:8080/api/students will take you to the Azure AD login page and after successful authentication, you should be able to call the above APIs.
