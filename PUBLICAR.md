# Publicación futura desde PowerShell

No se ha creado ningún remoto ni se ha hecho push. Ejecutar estos bloques solo
cuando se haya elegido la cuenta y autorizado la publicación correspondiente.
Se usan dos repositorios nuevos, público y privado, con los mismos commits.
No crear un README, licencia ni `.gitignore` remotos al inicializarlos.

Requisitos: Git y GitHub CLI (`gh`). `gh` no estaba disponible en el PATH durante
la preparación; instalarlo por el procedimiento habitual antes de estos comandos.
También se pueden crear repositorios vacíos desde GitHub y usar únicamente los
comandos Git; establecer después `develop` en Settings → Default branch.

## Preparación

```powershell
Set-Location 'C:\Users\TecdataEngineering\IdeaProjects\secu-fix28-minimo'
$cuenta = 'CUENTA_AUTORIZADA'  # Usuario u organización; sustituir antes de ejecutar.
$repoPublico = 'secu-fix28-publico'
$repoPrivado = 'secu-fix28-privado'
if ($cuenta -eq 'CUENTA_AUTORIZADA') { throw 'Indica la cuenta autorizada' }

gh auth login --hostname github.com --git-protocol https --web
if ($LASTEXITCODE -ne 0) { throw 'Falló la autenticación de GitHub CLI' }
gh auth status
gh auth setup-git
git switch develop
git status --short
git log --all --oneline --decorate
git remote -v
# Debe estar limpio y sin remotos en la primera publicación.
```

La autenticación de `gh` es la del operador que publica, independiente de la
credencial de la conexión GitHub que utilizará SecuenceIQ y el Sensor.
No escribir tokens en comandos, README, URLs remotas ni archivos del proyecto.

## Primero: repositorio público

```powershell
gh repo create "$cuenta/$repoPublico" --public --source . --remote publico `
    --description 'Fixture minima de Fix-28; SQL inseguro aislado para analisis estatico'
if ($LASTEXITCODE -ne 0) { throw 'No se creó el repositorio público; no continuar' }

git push publico develop prueba-fix28
if ($LASTEXITCODE -ne 0) { throw 'Falló el push público; no continuar' }
gh repo edit "$cuenta/$repoPublico" --default-branch develop
if ($LASTEXITCODE -ne 0) { throw 'No se pudo configurar develop como predeterminada' }
git remote set-head publico develop

gh api "repos/$cuenta/$repoPublico" `
    --jq '{id,full_name,private,default_branch,clone_url,html_url}'
gh api "repos/$cuenta/$repoPublico/branches" `
    --jq '.[] | {name,sha:.commit.sha}'
git ls-remote --symref publico HEAD refs/heads/develop refs/heads/prueba-fix28
```

Esperado: `private=false`, `default_branch=develop`, ambas ramas y los SHA locales.
Completar público/develop y público/prueba-fix28 antes de la segunda publicación.

## Después: repositorio privado

Ejecutar solo cuando la publicación privada también esté autorizada:

```powershell
gh repo create "$cuenta/$repoPrivado" --private --source . --remote privado `
    --description 'Fixture minima privada para probar Fix-28 desde el Sensor'
if ($LASTEXITCODE -ne 0) { throw 'No se creó el repositorio privado; no continuar' }

git push privado develop prueba-fix28
if ($LASTEXITCODE -ne 0) { throw 'Falló el push privado; no continuar' }
gh repo edit "$cuenta/$repoPrivado" --default-branch develop
if ($LASTEXITCODE -ne 0) { throw 'No se pudo configurar develop como predeterminada' }
git remote set-head privado develop

gh api "repos/$cuenta/$repoPrivado" `
    --jq '{id,full_name,private,default_branch,clone_url,html_url}'
gh api "repos/$cuenta/$repoPrivado/branches" `
    --jq '.[] | {name,sha:.commit.sha}'
git ls-remote --symref privado HEAD refs/heads/develop refs/heads/prueba-fix28
```

Esperado: `private=true` y otro `id` remoto; los SHA de las ramas deben coincidir
con los del público. No cambiar la visibilidad del público ni mezclar asociaciones.
No usar `--force`, `--mirror` ni `--push` implícito en `gh repo create`.

Fuentes: [crear con gh](https://cli.github.com/manual/gh_repo_create),
[editar la rama predeterminada](https://cli.github.com/manual/gh_repo_edit),
[metadatos REST](https://docs.github.com/en/rest/repos/repos#get-a-repository),
[ramas REST](https://docs.github.com/en/rest/branches/branches#list-branches).
