---
origem: local
precedencia: local-vence
versao: 2.1.0
---

# Recipe: Upload de Arquivo S3 Ponta-a-Ponta

**Objetivo:** Implementar upload de arquivo via presigned URL do S3, com validação de existência antes de persistir a referência no banco.

**Pré-requisitos:**
- `gems-aws` + `gems-aws-web` (S3Controller, S3Service)
- `gems-exception` (envelope de resposta)

**Rules relacionadas:**
- [storage-s3.md](../rules/backend-java/storage-s3.md)
- [services.md](../rules/backend-java/services.md)
- [forms.md](../rules/frontend-angular/forms.md)
- [feedback-alerts.md](../rules/frontend-angular/feedback-alerts.md)

---

## Fluxo Completo

```
Frontend                    Backend                     S3
   |                            |                        |
   |-- POST /generate-upload-url (fileKey) -->            |
   |                            |-- presignedPutUrl ---->|
   |<-- presignedUrl + fileKey --                        |
   |                                                     |
   |-- PUT presignedUrl (arquivo binário) ------------->|
   |<-- 200 OK -----------------------------------------|
   |                                                     |
   |-- POST /api/{recurso} (dto com fileKey) -->         |
   |           |-- s3Service.fileExists(fileKey) ------->|
   |           |<-- true/false --------------------------|
   |           |-- repository.save() -------------------|
   |<-- 201 Created ----------------------------------------
```

---

## Passo 1: Gerar Presigned URL (Backend — reutilizar S3Controller)

O `gems-aws-web` já fornece `S3Controller` com endpoint `/generate-upload-url`. Configure apenas o bucket:

```yaml
# application.yml
gems:
  aws:
    s3:
      bucket: meu-bucket-{app}
      region: us-east-1
```

O endpoint retorna:
```json
{
  "presignedUrl": "https://meu-bucket.s3.amazonaws.com/...",
  "fileKey": "uploads/{uuid}/{nome-original}"
}
```

## Passo 2: DTO com fileKey

```java
// {Entidade}DTO.java — adicionar campo fileKey
@Data
public class {Entidade}DTO {
    private UUID id;
    private String nome;
    private String fileKey;   // Chave do arquivo no S3 (nunca a URL pública)
}
```

## Passo 3: Validar Existência no Service

```java
@Transactional(rollbackFor = Exception.class)
public {Entidade} insert({Entidade}DTO dto) {
    validate(dto);
    {Entidade} entidade = mapper.toEntity(dto);
    return repository.save(entidade);
}

private void validate({Entidade}DTO dto) {
    List<String> errors = new ArrayList<>();
    if (ObjectUtil.isNullOrEmpty(dto.getNome()))
        errors.add("Nome é obrigatório.");
    if (ObjectUtil.isNullOrEmpty(dto.getFileKey()))
        errors.add("Arquivo é obrigatório.");
    else if (!s3Service.fileExists(dto.getFileKey()))
        errors.add("Arquivo não encontrado no servidor. Faça o upload novamente.");
    if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
        throw new BusinessException( errors );
    }
}
```

`s3Service.fileExists(fileKey)` é obrigatório antes de persistir — garante que o arquivo chegou ao S3 antes de salvar a referência.

## Passo 4: URL de Download

Para gerar URL temporária de download (quando necessário exibir o arquivo):

```java
@GetMapping("/{id}/download")
public ResponseEntity<String> getDownloadUrl( @PathVariable UUID id ) {
    var dto = service.findById( id );
    var url = s3Service.generatePresignedGetUrl( dto.getFileKey(), Duration.ofMinutes( 15 ) );
    return ResponseEntity.ok( url );
}
```

## Passo 5: Componente Angular — Upload Flow

```typescript
@Component({ standalone: true, ... })
export class {Entidade}FormComponent {
  private s3Store    = inject(S3Store);         // Store para /generate-upload-url
  private store      = inject({Entidade}Store);
  private alertService = inject(AlertService);

  isUploading = signal(false);
  isSaving    = signal(false);
  fileKey     = signal<string | null>(null);
  fileName    = signal<string | null>(null);

  async onFileSelect(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.isUploading.set(true);
    try {
      // 1. Obter presigned URL
      const { presignedUrl, fileKey } = await firstValueFrom(
        this.s3Store.generateUploadUrl(file.name)
      );

      // 2. Fazer PUT direto no S3
      await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: { 'Content-Type': file.type }
      });

      this.fileKey.set(fileKey);
      this.fileName.set(file.name);
      this.alertService.success('Upload concluído', 'Arquivo enviado com sucesso.');
    } catch (err) {
      this.alertService.error('Erro', 'Falha ao fazer upload. Tente novamente.');
    } finally {
      this.isUploading.set(false);
    }
  }

  save(): void {
    if (!this.fileKey()) {
      this.alertService.warning('Atenção', 'Faça o upload do arquivo antes de salvar.');
      return;
    }
    this.isSaving.set(true);
    const dto = { nome: this.nome(), fileKey: this.fileKey()! };
    this.store.create(dto).subscribe({
      next: () => {
        this.alertService.success('Sucesso', 'Registro criado com sucesso.');
        this.nav.navigate(['/{modulo}/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isSaving.set(false);
      }
    });
  }
}
```

```html
<!-- Template do campo de upload -->
<div class="form-group full-width">
  <label class="required fw-bold">Arquivo</label>
  <input type="file" class="form-control" (change)="onFileSelect($event)"
         [disabled]="isUploading()">
  @if (isUploading()) {
    <small class="text-muted">
      <i class="fa-solid fa-circle-notch fa-spin"></i> Enviando arquivo...
    </small>
  }
  @if (fileName()) {
    <small class="text-success">
      <i class="fa-solid fa-check"></i> {{ fileName() }}
    </small>
  }
</div>
```

---

## Checklist de Conformidade

- [ ] Nunca salvar URL pública — sempre salvar `fileKey` (path relativo no S3).
- [ ] `s3Service.fileExists(fileKey)` obrigatório no `validate()` do Service.
- [ ] Upload via `fetch` direto na `presignedUrl` (PUT, não POST).
- [ ] `Content-Type` incluído no PUT para o S3.
- [ ] Estado de upload separado do estado de salvamento (`isUploading` + `isSaving`).
- [ ] Feedback visual durante upload (`fa-circle-notch fa-spin`).
- [ ] URL de download gerada on-demand com TTL (`Duration.ofMinutes(15)`) — nunca URL permanente.
