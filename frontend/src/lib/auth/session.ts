type TokenProvider = () => Promise<string | null>;
type LoginHandler = () => Promise<void>;

let tokenProvider: TokenProvider | null = null;
let loginHandler: LoginHandler | null = null;

export function setTokenProvider(provider: TokenProvider | null) {
  tokenProvider = provider;
}

export function setLoginHandler(handler: LoginHandler | null) {
  loginHandler = handler;
}

export async function getAccessToken() {
  return tokenProvider?.() ?? null;
}

export async function redirectToLogin() {
  await loginHandler?.();
}
