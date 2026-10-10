import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getCurrentUser, switchCurrentUser } from '../api/auth.js';

/**
 * 全局当前用户 Context。
 *
 * <p>由 main.jsx 注入 Provider；各页面通过 {@link useCurrentUser} 读取。
 * 切换调用 {@link switchUser}：写本地态 + 调服务端 {@code POST /api/auth/switch}，
 * 服务端下发 SESSION cookie，后续 /api/interact 即以此用户身份执行。</p>
 */

const CurrentUserContext = createContext({
  userId: null,
  loading: true,
  switchUser: async () => {},
  refresh: async () => {},
});

export function CurrentUserProvider({ children }) {
  const [userId, setUserId] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const id = await getCurrentUser();
    setUserId(id);
    setLoading(false);
    return id;
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const switchUser = useCallback(
    async (next) => {
      // 持久化由服务端 Set-Cookie 完成；前端只更新本地态
      const saved = await switchCurrentUser(next);
      setUserId(saved ?? next);
      return saved;
    },
    []
  );

  return (
    <CurrentUserContext.Provider value={{ userId, loading, switchUser, refresh }}>
      {children}
    </CurrentUserContext.Provider>
  );
}

export function useCurrentUser() {
  return useContext(CurrentUserContext);
}