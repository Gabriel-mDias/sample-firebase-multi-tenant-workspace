package br.com.gems.app.administracao.service;

import br.com.gems.app.model.dto.UsuarioCadastroDTO;
import br.com.gems.app.model.dto.TenantCadastroDTO;
import br.com.gems.firebase.auth.AdministrationViews;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.SecurityAdministrationReader;
import br.com.gems.firebase.auth.TenantAdministration;
import br.com.gems.firebase.auth.TenantAdministratorBootstrap;
import br.com.gems.firebase.auth.UserAdministration;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.EmailUtil;
import br.com.gems.utils.ObjectUtil;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplicação só coordena portas tipadas; identidade/grants/outbox são algoritmos da SDK. */
@Service
@RequiredArgsConstructor
public class AdministracaoService {
    private final UserAdministration users;
    private final TenantAdministration tenants;
    private final GrantAdministration grants;
    private final SecurityAdministrationReader reader;
    private final TenantAdministratorBootstrap bootstrap;
    @PreAuthorize("hasRole('CONSULTAR_USUARIO')")
    public AdministrationViews.Page<AdministrationViews.User> users( AdministrationViews.Query query ) {
        return reader.listUsers( query );
    }
    @PreAuthorize("hasRole('CONSULTAR_TENANT')")
    public AdministrationViews.Page<AdministrationViews.Tenant> tenants( AdministrationViews.Query query ) {
        return reader.listTenants( query );
    }
    @PreAuthorize("hasRole('CONSULTAR_MEMBERSHIP')")
    public AdministrationViews.Page<AdministrationViews.Membership> memberships( UUID tenant,
            AdministrationViews.Query query ) {
        return reader.listMemberships( tenant, query );
    }
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    public AdministrationViews.Page<AdministrationViews.Grant> grants( UUID tenant, AdministrationViews.Query query ) {
        return reader.listGrants( tenant, query );
    }
    @PreAuthorize("hasRole('INSERIR_USUARIO')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID createUser( UsuarioCadastroDTO dto ) {
        validate( dto );
        return dto.method() == UsuarioCadastroDTO.Method.GOOGLE
                ? users.preProvisionGoogle( dto.name(), dto.email(), "CONSULTAR_PROPRIA_CONTA" )
                : users.provisionPassword( dto.name(), dto.email(), "CONSULTAR_PROPRIA_CONTA" );
    }
    @PreAuthorize("hasRole('ALTERAR_USUARIO')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void updateUser( UUID user, String name, String email ) {
        validate( new UsuarioCadastroDTO( name, email, UsuarioCadastroDTO.Method.GOOGLE ) );
        users.update( user, name, email );
    }
    @PreAuthorize("hasRole('SUSPENDER_USUARIO')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void suspendUser( UUID user ) {
        users.suspend( user );
    }
    @PreAuthorize("hasRole('REATIVAR_USUARIO')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void reactivateUser( UUID user ) {
        users.requestReactivation( user );
    }
    @PreAuthorize("hasRole('INSERIR_TENANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID createTenant( TenantCadastroDTO dto ) {
        validate( dto );
        return tenants.create( dto.alias(), dto.name() );
    }
    @PreAuthorize("hasRole('ALTERAR_TENANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void renameTenant( UUID tenant, String name ) {
        validate( name );
        tenants.rename( tenant, name );
    }
    @PreAuthorize("hasRole('SUSPENDER_TENANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void suspendTenant( UUID tenant ) {
        tenants.suspend( tenant );
    }
    @PreAuthorize("hasRole('CRIAR_ADMINISTRADOR_TENANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID firstAdministrator( UUID tenant, UUID user, Set<String> actions ) {
        return bootstrap.initialize( tenant, user, actions );
    }
    @PreAuthorize("hasRole('INSERIR_MEMBERSHIP')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID addMember( UUID tenant, UUID user ) {
        return tenants.addMember( tenant, user, "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );
    }
    @PreAuthorize("hasRole('ENCERRAR_MEMBERSHIP')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void endMember( UUID tenant, UUID member ) {
        tenants.endMembership( tenant, member );
    }
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID grant( UUID tenant, UUID member, String action ) {
        return grants.grant( GrantAdministration.Target.MEMBER, tenant, member, action );
    }
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void revoke( UUID tenant, UUID grant ) {
        grants.revoke( GrantAdministration.Target.MEMBER, tenant, grant );
    }
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    public AdministrationViews.Page<AdministrationViews.Profile> profiles( UUID tenant,
            AdministrationViews.Query query ) {
        return reader.listProfiles( tenant, query );
    }
    @PreAuthorize("hasRole('CONSULTAR_GRANT')")
    public AdministrationViews.Page<AdministrationViews.Group> groups( UUID tenant, AdministrationViews.Query query ) {
        return reader.listGroups( tenant, query );
    }
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID createProfile( UUID tenant, String code, String name ) {
        validate( code, name );
        return grants.createProfile( tenant, code, name );
    }
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID createGroup( UUID tenant, String code, String name ) {
        validate( code, name );
        return grants.createGroup( tenant, code, name );
    }
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID grantTo( GrantAdministration.Target target, UUID tenant, UUID owner, String action ) {
        return grants.grant( target, tenant, owner, action );
    }
    @PreAuthorize("hasRole('CONCEDER_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID link( GrantAdministration.Link link, UUID tenant, UUID left, UUID right ) {
        return grants.link( link, tenant, left, right );
    }
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void revokeTarget( GrantAdministration.Target target, UUID tenant, UUID id ) {
        grants.revoke( target, tenant, id );
    }
    @PreAuthorize("hasRole('REVOGAR_GRANT')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void revokeLink( GrantAdministration.Link link, UUID tenant, UUID id ) {
        grants.revoke( link, tenant, id );
    }

    @PreAuthorize("hasRole('CONSULTAR_ACAO_GLOBAL')")
    public AdministrationViews.Page<AdministrationViews.Grant> globalGrants( UUID user,
            AdministrationViews.Query query ) {
        return reader.listGlobalGrants( user, query );
    }
    @PreAuthorize("hasRole('CONCEDER_ACAO_GLOBAL')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public UUID grantGlobal( UUID user, String action ) {
        return grants.grantGlobal( user, action );
    }
    @PreAuthorize("hasRole('REVOGAR_ACAO_GLOBAL')")
    @Transactional(transactionManager = "firebaseSecurityTransactionManager", rollbackFor = Exception.class)
    public void revokeGlobal( UUID user, UUID grant ) {
        grants.revokeGlobal( user, grant );
    }
    private void validateField( String value, String field, int maximum, ArrayList<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( value ) || value.isBlank() || value.length() > maximum ) {
            errors.add( "Informe " + field + " de até " + maximum + " caracteres." );
        }
    }
    private void validate( TenantCadastroDTO dto ) {
        var errors = new ArrayList<String>();
        if ( ObjectUtil.isNullOrEmpty( dto ) ) {
            errors.add( "Informe a empresa." );
        } else {
            validateField( dto.alias(), "alias", 56, errors );
            validateField( dto.name(), "nome", 255, errors );
        }
        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }
    private void validate( String name ) {
        var errors = new ArrayList<String>();
        validateField( name, "nome", 255, errors );
        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }
    private void validate( String code, String name ) {
        var errors = new ArrayList<String>();
        validateField( code, "código", 128, errors );
        validateField( name, "nome", 255, errors );
        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }
    private void validate( UsuarioCadastroDTO dto ) {
        var errors = new ArrayList<String>();
        if ( ObjectUtil.isNullOrEmpty( dto ) ) {
            errors.add( "Informe o usuário." );
        } else {
            validateNomeUsuario( dto, errors );
            validateEmailUsuario( dto, errors );
            validateMetodoUsuario( dto, errors );
        }
        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }
    private void validateNomeUsuario( UsuarioCadastroDTO dto, ArrayList<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( dto.name() ) || dto.name().isBlank() || dto.name().length() > 150 ) {
            errors.add( "Informe nome de até 150 caracteres." );
        }
    }
    private void validateEmailUsuario( UsuarioCadastroDTO dto, ArrayList<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( dto.email() ) || !EmailUtil.isValid( dto.email() )
                || dto.email().length() > 254 ) {
            errors.add( "Informe e-mail válido de até 254 caracteres." );
        }
    }
    private void validateMetodoUsuario( UsuarioCadastroDTO dto, ArrayList<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( dto.method() ) ) {
            errors.add( "Informe Google ou senha." );
        }
    }
}
