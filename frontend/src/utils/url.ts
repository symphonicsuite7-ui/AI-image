const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export function resolveImageUrl(imageUrl: string): string {
  if (!imageUrl || /^(https?:|data:|blob:)/i.test(imageUrl) || !apiBaseUrl) {
    return imageUrl
  }
  return `${apiBaseUrl}${imageUrl.startsWith('/') ? '' : '/'}${imageUrl}`
}
